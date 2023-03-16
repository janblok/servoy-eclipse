import { Injectable, } from '@angular/core';
import { WindowRefService, SessionStorageService, Deferred, LoggerService, LoggerFactory, Locale, RequestInfoPromise } from '@servoy/public';
import { WebsocketService, WebsocketSession } from '../sablo/websocket.service';
import { ConverterService } from './converter.service';

@Injectable({
  providedIn: 'root'
})
export class SabloService {

    private locale: Locale = null;
    private wsSession: WebsocketSession;
    private currentServiceCallCallbacks = [];
    private currentServiceCallDone: boolean;
    private currentServiceCallWaiting = 0;
    private currentServiceCallTimeouts;
    private log: LoggerService;


    constructor(private websocketService: WebsocketService, private sessionStorage: SessionStorageService, private converterService: ConverterService,
        private windowRefService: WindowRefService, logFactory: LoggerFactory) {
        this.log = logFactory.getLogger('SabloService');
        this.windowRefService.nativeWindow.window.addEventListener('beforeunload', () => {
            sessionStorage.remove('svy_session_lock');
        });
        this.windowRefService.nativeWindow.window.addEventListener('pagehide', () => {
            sessionStorage.remove('svy_session_lock');
        });

        if (sessionStorage.has('svy_session_lock')) {
            this.clearSabloInfo()
            this.log.warn('Found a lock in session storage. The storage was cleared.');
        }

        sessionStorage.set('svy_session_lock', '1');
    }

    public connect(context, queryArgs, websocketUri): WebsocketSession {
        const wsSessionArgs = {
            context,
            queryArgs,
            websocketUri
        };

        this.wsSession = this.websocketService.connect(wsSessionArgs.context, [this.getClientnr(), this.getWindowName(), this.getWindownr()], wsSessionArgs.queryArgs, wsSessionArgs.websocketUri);

        this.wsSession.onMessageObject((msg) => {
            // data got back from the server
            if (msg.clientnr) {
                this.sessionStorage.set('clientnr', msg.clientnr);
            }
            if (msg.windownr) {
                this.sessionStorage.set('windownr', msg.windownr);
            }
            if (msg.clientnr || msg.windownr) {
                // update the arguments on the reconnection websocket.
                this.websocketService.setConnectionPathArguments([this.getClientnr(), this.getWindowName(), this.getWindownr()]);
            }
        });

        return this.wsSession;
    }

    public createDeferredWSEvent(): { deferred: Deferred<any>; cmsgid: number } {
        return this.wsSession.createDeferredEvent();
    }

    public resolveDeferedEvent(cmsgid: number, argument: any, success: boolean) {
        this.wsSession.resolveDeferedEvent(cmsgid, argument, success);
    }

    public getClientnr() {
        const sessionnr = this.sessionStorage.get('clientnr');
        if (sessionnr) {
            return sessionnr;
        }
        return this.websocketService.getURLParameter('clientnr');
    }

    public getWindowName() {
        return this.websocketService.getURLParameter('windowname');
    }

    public getWindownr() {
        return this.sessionStorage.get('windownr');
    }

    public getWindowUrl(windowname: string) {
        return 'index.html?windowname=' + encodeURIComponent(windowname) + '&clientnr=' + this.getClientnr();
    }

    public getLanguageAndCountryFromBrowser() {
        let langAndCountry;
        const browserLanguages = this.windowRefService.nativeWindow.navigator['languages'];
        // this returns first one of the languages array if the browser supports this (Chrome and FF) else it falls back to language or userLanguage
        // (IE, and IE seems to return the right one from there)
        if (browserLanguages && browserLanguages.length > 0) {
            langAndCountry = browserLanguages[0];
            if (browserLanguages.length > 1 && langAndCountry.indexOf('-') === -1
                && browserLanguages[1].indexOf(langAndCountry + '-') === 0) {
                // if the first language in the list doesn't specify country, see if the following one is the same language but with a country specified
                // (for example browser could give a list of "en", "en-GB", ...)
                langAndCountry = browserLanguages[1];
            }
        } else {
            langAndCountry = (this.windowRefService.nativeWindow.navigator.language || this.windowRefService.nativeWindow.navigator['userLanguage']);
        }
        // in some weird scenario in firefox is not set, default it to en
        if (!langAndCountry) langAndCountry = 'en';
        return langAndCountry;
    }
    public getLocale(): Locale {
        if (!this.locale) {
            const langAndCountry = this.getLanguageAndCountryFromBrowser();
            const array = langAndCountry.split('-');
            this.locale = { language: array[0], country: array[1], full: langAndCountry };
        }
        return this.locale;
    }

    public setLocale(loc: Locale) {
        this.locale = loc;
    }

    /**
     * IMPORTANT!
     * 
     * If the returned value is a promise and if the caller is INTERNAL code that chains more .then() or other methods and returns the new promise
     * to it's own callers, it MUST to wrap the new promise (returned by that then() for example) using $websocket.wrapPromiseToPropagateCustomRequestInfoInternal().
     * 
     * This is so that the promise that ends up in (3rd party or our own) components and service code - that can then set .requestInfo on it - ends up to be
     * propagated into the promise that this callService(...) registered in "deferredEvents"; that is where any user set .requestInfo has to end up, because
     * that is where getCurrentRequestInfo() gets it from. And that is where special code - like foundset listeners also get the current request info from to
     * return it back to the user (component/service code).   
     */
    public callService<T>(serviceName: string, methodName: string, argsObject, async?: boolean): RequestInfoPromise<T> {
        const promise = this.wsSession.callService<T>(serviceName, methodName, argsObject, async);
        return async ? promise : this.waitForServiceCallbacks<T>(promise, [100, 200, 500, 1000, 3000, 5000]);
    }

    public addToCurrentServiceCall(func: () => void) {
        if (this.currentServiceCallWaiting === 0) {
            // No service call currently running, call the function now
            setTimeout(() => {
                func();
            });
        } else {
            this.currentServiceCallCallbacks.push(func);
        }
    }

    public sendServiceChangesJSON(serviceName: string, changes: any) {
        this.wsSession.sendMessageObject({ servicedatapush: serviceName, changes });
    }

    public addIncomingMessageHandlingDoneTask(func: () => any) {
        if (this.wsSession) this.wsSession.addIncomingMessageHandlingDoneTask(func);
        else {
            // it can be undefined in form designer, as updates from server come via a different route and this.websocketService.connect() is called from a different place 
            this.websocketService.getSession().then((session) => session.addIncomingMessageHandlingDoneTask(func));
        }
    }

    public getCurrentRequestInfo(): any {
        return this.websocketService.getCurrentRequestInfo();
    }

    private callServiceCallbacksWhenDone() {
        if (this.currentServiceCallDone || --this.currentServiceCallWaiting === 0) {
            this.currentServiceCallWaiting = 0;
            this.currentServiceCallTimeouts.map((id) => clearTimeout(id));
            const tmp = this.currentServiceCallCallbacks;
            this.currentServiceCallCallbacks = [];
            tmp.map((func: () => void) => {
                func();
            });
        }
    }

    private waitForServiceCallbacks<T>(promise: Promise<T>, times: number[]): RequestInfoPromise<T>{
        if (this.currentServiceCallWaiting > 0) {
            // Already waiting
            return promise;
        }

        this.currentServiceCallDone = false;
        this.currentServiceCallWaiting = times.length;
        this.currentServiceCallTimeouts = times.map((t) => setTimeout(this.callServiceCallbacksWhenDone, t));

        return this.websocketService.wrapPromiseToPropagateCustomRequestInfoInternal(promise, promise.then((arg) => {
                this.currentServiceCallDone = true;
                return arg;
            }, (arg) => {
                this.currentServiceCallDone = true;
                return Promise.reject(arg);
            }));
    }

//    private getAPICallFunctions(call, formState) {
//        let funcThis: Record<string, () => any>;
//        if (call.viewIndex !== undefined) {
//            // I think this viewIndex' is never used; it was probably intended for components with multiple rows targeted by the same component if
//            // it wants to allow calling API on non-selected rows, but it is not used
//            funcThis = formState.api[call.bean][call.viewIndex];
//        } else if (call.propertyPath !== undefined) {
//            // handle nested components; the property path is an array of string or int keys going
//            // through the form's model starting with the root bean name, then it's properties (that could be nested)
//            // then maybe nested child properties and so on
//            let obj = formState.model;
//            for (const pp of call.propertyPath) obj = obj[pp];
//            funcThis = obj.api;
//        } else {
//            funcThis = formState.api[call.bean];
//        }
//        return funcThis;
//    }

    private clearSabloInfo() {
        this.sessionStorage.remove('windownr');
        this.sessionStorage.remove('clientnr');
    }

}
