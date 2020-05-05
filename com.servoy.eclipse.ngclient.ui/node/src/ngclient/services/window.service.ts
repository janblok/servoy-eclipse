import { Injectable, ComponentFactoryResolver, Injector, ApplicationRef, RendererFactory2, Inject, Renderer2 } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { FormService } from '../form.service';
import { ServoyService } from '../servoy.service';
import { DialogWindowComponent } from './dialog-window/dialog-window.component';
import { BSWindowManager } from './bootstrap-window/bswindow_manager.service';
import { BSWindow } from './bootstrap-window/bswindow.service';
import { WindowRefService } from '../../sablo/util/windowref.service';
import { LocalStorageService } from 'angular-web-storage';
import { SabloService } from '../../sablo/sablo.service';
import { DOCUMENT, PlatformLocation } from '@angular/common';
import { ApplicationService } from './application.service';
import { Router, NavigationStart } from '@angular/router';
import { ServoyApi } from '../servoy_api';
import { SessionStorageService } from 'angular-web-storage';
import { WebsocketService } from '../../sablo/websocket.service';

@Injectable()
export class WindowService {
    public static readonly WINDOW_TYPE_DIALOG = 0;
    public static readonly WINDOW_TYPE_MODAL_DIALOG = 1;
    public static readonly WINDOW_TYPE_WINDOW = 2;


    private instances: any = {};
    private renderer: Renderer2;
    private windowCounter: number;
    private windowsRestored = false;

    constructor(private formService: FormService,
        public servoyService: ServoyService,
        private windowRefService: WindowRefService,
        private componentFactoryResolver: ComponentFactoryResolver,
        private _applicationRef: ApplicationRef,
        private _injector: Injector,
        public localStorageService: LocalStorageService,
        public sessionStorageService: SessionStorageService,
        private titleService: Title,
        public sabloService: SabloService,
        private bsWindowManager: BSWindowManager,
        private rendererFactory: RendererFactory2,
        private appService: ApplicationService,
        private platformLocation: PlatformLocation,
        private webSocketService: WebsocketService,
        @Inject(DOCUMENT) private document: any) {

        this.renderer = rendererFactory.createRenderer(null, null);
        this.platformLocation.onPopState((event) => {
            this.formService.goToForm(this.platformLocation.hash.replace('#', ''));
        });

        this.windowCounter = 0;
    }

    private restoreWindows() {
        const window0 = this.sessionStorageService.get('window0');
        if (window0 && window0.showForm) {
            const isConnected = false;
            // wait until the server is connected
            const interval = setInterval(() => {
                if (this.webSocketService.isConnected()) {
                    clearInterval(interval);
                    let counter = 0;
                    let windowCounterReset = false;
                    while (this.sessionStorageService.get('window' + counter)) {
                        const window = this.sessionStorageService.get('window' + counter);
                        // server call for getting form's data (send data from server to client)
                        // call a couple of methods that will create and display the window
                        this.create(window.name, window.type);
                        this.switchForm(window.name, window.switchForm, window.navigatorForm);
                        this.setTitle(window.name, window.title);
                        this.sabloService.callService('$windowService', 'touchForm', { name: window.showForm }, false).then( () => {
                          // in order to show all the windows the counter must be reset 
                          if (this.windowsRestored && !windowCounterReset) {
                              this.windowCounter = 0;
                              windowCounterReset = true;
                          }
                          this.show(window.name, window.showForm, window.showTitle);
                          this.windowCounter++;
                        });
                        counter++;
                        this.windowCounter++;
                }
                }
            }, 1000);
        }
    }

    public updateController(formName, formStructure) {
        const formState = JSON.parse(formStructure)[formName];
        this.formService.createFormCache(formName, formState);
    }

    public create(name: string, type: number) {
        const storedWindow = this.sessionStorageService.get('window' + this.windowCounter);
        if (!storedWindow || storedWindow.name != name) {
            this.sessionStorageService.set('window' + this.windowCounter, { 
                name: name,
                type: type 
            });
        }
        if (!this.instances[name]) {
            this.instances[name] = new SvyWindow(name, type, this);
        }
    }
  
    public show(name: string, form: string, title: string) { 
        const currentWindow = 'window' + this.windowCounter;
        const storedWindow = this.sessionStorageService.get(currentWindow)
        if (storedWindow && !storedWindow.showForm) {
            storedWindow.showForm = form;
            storedWindow.showTitle = title;
            this.sessionStorageService.set(currentWindow, storedWindow);
            this.windowCounter++;
        }
        const instance = this.instances[name];
        if ( this.instances[name] ) {
            instance.title = title;
            if ( instance.bsWindowInstance ) {
                // already showing
                return;
            }

            if ([...this.document.getElementsByClassName('svy-window')].length < 1) {
                const customEvent = new CustomEvent('disableTabseq', {
                    bubbles: true
                  });
                this.document.getElementById('mainForm').dispatchEvent(customEvent);
            }

            if ( instance.storeBounds ) {
                instance.location = this.localStorageService.get(
                    this.servoyService.getSolutionSettings().solutionName + name + '.storedBounds.location');
                instance.size = this.localStorageService.get(
                        this.servoyService.getSolutionSettings().solutionName + name + '.storedBounds.size');
            }

            // resolve initial bounds
            let location = null;
            let size = instance.form.size;
            if ( instance.initialBounds ) {
                const bounds = instance.initialBounds;
                if (bounds.x > -1 && bounds.y > -1) {
                    location = {
                        x: bounds.x,
                        y: bounds.y
                    };
                }
                if  (bounds.width > 0 && bounds.height > 0) {
                    size = { width: bounds.width, height: bounds.height };
                }
            }
            if ( instance.location ) {
                location = instance.location;
            }
            if ( instance.size ) {
                size = instance.size;
            }
            // -1 means default size and location(center)
            let formSize = size;
            if ( !formSize || ( formSize.width === -1 && formSize.height === -1 ) )
                formSize = instance.form.size;

            const windowWidth = this.windowRefService.nativeWindow.document.documentElement.clientWidth;
            const windowHeight = this.windowRefService.nativeWindow.document.documentElement.clientHeight;

            // this can happen in case of responsive forms
            if ( formSize.width === 0 ) formSize.width = windowWidth / 2;
            if ( formSize.height === 0 ) formSize.height = windowHeight / 2;


            if ( !location || ( location.x < 0 && location.y < 0 ) ) location = this.centerWindow( formSize );
            if ( !size || size.width < 0 || size.height < 0 ) size = null;

            if ( size ) {
                // dialog shouldn't be bigger than viewport
                if ( size.width && size.width > windowWidth ) {
                    size.width = windowWidth;
                }
                if ( size.height && size.height > windowHeight ) {
                    size.height = windowHeight;
                }
            }
            // convert servoy x,y to library top , left
            const loc = { left: location.x, top: location.y };

            // create the bs window instance
            const componentFactory = this.componentFactoryResolver.resolveComponentFactory(DialogWindowComponent);
            const dialogWindowComponent = componentFactory.create(this._injector);
            dialogWindowComponent.instance.setWindow(instance);
            this._applicationRef.attachView(dialogWindowComponent.hostView);

            const opt = {
                id: instance.name,
                fromElement: dialogWindowComponent.location.nativeElement.childNodes[0],
                title: instance.title,
                resizable: !!instance.resizable,
                location: loc,
                size: size,
                isModal: instance.type === WindowService.WINDOW_TYPE_MODAL_DIALOG
            };
            instance.bsWindowInstance = this.bsWindowManager.createWindow(opt);

            instance.bsWindowInstance.element.addEventListener( 'bswin.resize', (event, size) => { instance.onResize(event, size); } );
            instance.bsWindowInstance.element.addEventListener( 'bswin.move', (event, location) => { instance.onResize(event, location); } );
            instance.bsWindowInstance.element.addEventListener( 'bswin.active', (event) => {
                const customEvent = new CustomEvent(event.detail.active ? 'enableTabseq' : 'disableTabseq', {
                    bubbles: true
                  });
                event.target.dispatchEvent(customEvent);
            } );
            [...this.document.getElementsByClassName('window-header')][0].focus();
            instance.bsWindowInstance.setActive( true );
            // init the size of the dialog
            const width = instance.bsWindowInstance.element.getBoundingClientRect().width;
            const height = instance.bsWindowInstance.element.getBoundingClientRect().height;
            if ( width > 0 && height > 0 ) {
                const dialogSize = { width: width, height: height };
                this.sabloService.callService( '$windowService', 'resize', { name: instance.name, size: dialogSize }, true );
            }
        }
    }

    public hide(name: string) {
        let winCounter = 0;
        while (this.sessionStorageService.get('window' + winCounter)) {
            const window = this.sessionStorageService.get('window' + winCounter);
            if (window.name === name) {
                this.sessionStorageService.remove('window' + winCounter);
                this.windowCounter--;
            }
            winCounter++;
        }
        const instance = this.instances[name];
        if (instance) {
            instance.hide();
            if ([...this.document.getElementsByClassName('svy-window')].length < 1) {
                const customEvent = new CustomEvent('enableTabseq', {
                    bubbles: true
                  });
                this.document.getElementById('mainForm').dispatchEvent(customEvent);
            }
        }
    }

    public destroy(name: string ) {
        const instance = this.instances[name];
        if ( instance ) {
            delete this.instances[name];
        }
    }

    public setTitle(name: string, title: string ) {
        const currentWindow = 'window' + this.windowCounter;
        const storedWindow = this.sessionStorageService.get(currentWindow);
        if (title && storedWindow && !storedWindow.title) {
            storedWindow.title = title;
            this.sessionStorageService.set(currentWindow, storedWindow);
        }
        if ( this.instances[name] && this.instances[name].type !== WindowService.WINDOW_TYPE_WINDOW ) {
            this.instances[name].title = title;
        } else {
            this.titleService.setTitle(title);
        }
    }

    public setInitialBounds(name: string, initialBounds: any ) {
        if ( this.instances[name] ) {
            this.instances[name].initialBounds = initialBounds;
        }
    }

    public setStoreBounds(name: string, storeBounds: boolean ) {
        if ( this.instances[name] ) {
            this.instances[name].storeBounds = storeBounds;
        }
    }

    public resetBounds(name: string ) {
        if ( this.instances[name] ) {
            this.instances[name].storeBounds = false;
            this.instances[name].clearBounds();
        }
    }

    public setLocation(name: string, location: any ) {
        if ( this.instances[name] ) {
            this.instances[name].setLocation( location );
        }
    }

    public setSize(name: string, size: any ) {
        if ( this.instances[name] ) {
            this.instances[name].setSize( size );
        }
    }

    public getSize(name: string ) {
        if ( this.instances[name] && this.instances[name].bsWindowInstance ) {
            return this.instances[name].getSize();
        } else {
            return { width: this.windowRefService.nativeWindow.innerWidth, height: this.windowRefService.nativeWindow.innerHeight };
        }
    }

    public setUndecorated(name: string, undecorated: boolean ) {
        if ( this.instances[name] ) {
            this.instances[name].undecorated = undecorated;
        }
    }

    public setCSSClassName(name: string, cssClassName: string ) {
        if ( this.instances[name] ) {
            this.instances[name].cssClassName = cssClassName;
        }
    }

    public setOpacity(name: string, opacity: boolean ) {
        if ( this.instances[name] ) {
            this.instances[name].opacity = opacity;
        }
    }

    public setResizable(name: string, resizable: boolean ) {
        if ( this.instances[name] ) {
            this.instances[name].resizable = resizable;
        }
    }

    public setTransparent(name: string, transparent: boolean ) {
        if ( this.instances[name] ) {
            this.instances[name].transparent = transparent;
        }
    }

    public toFront(name: string ) {
        if ( this.instances[name] ) {
            this.bsWindowManager.setFocused(this.instances[name]);
        }
    }

    public toBack(name: string ) {
        if ( this.instances[name] ) {
            this.bsWindowManager.sendToBack(this.instances[name]);
        }
    }

    centerWindow( formSize ) {
        // var body = $( 'body' );
        const windowWidth = this.windowRefService.nativeWindow.document.documentElement.clientWidth;
        const windowHeight = this.windowRefService.nativeWindow.document.documentElement.clientHeight;
        let top, left;
            // bodyTop = body.position().top + parseInt( body.css( 'paddingTop' ), 10 );
        left = ( windowWidth / 2 ) - ( formSize.width / 2 );
        top = ( windowHeight / 2 ) - ( formSize.height / 2 );
        // if ( top < bodyTop ) {
        //     top = bodyTop;
        // }
        if ( left < 0 ) left = 0;
        if ( top < 0 ) top = 0;
        return { x: left, y: top };
    }

    public switchForm(name, form, navigatorForm) {
        const currentWindow = 'window' + this.windowCounter;
        const storedWindow = this.sessionStorageService.get(currentWindow);
        if (storedWindow && !storedWindow.switchForm) {
            storedWindow.switchForm = form;
            storedWindow.navigatorForm = navigatorForm;
            this.sessionStorageService.set(currentWindow, storedWindow);
        }
        if (this.instances[name] && this.instances[name].type !== WindowService.WINDOW_TYPE_WINDOW) {
            this.instances[name].form = form;
            this.instances[name].navigatorForm = navigatorForm;
        }
        this.servoyService.loaded().then( () => {
            // if first show of this form in browser window then request initial data (dataproviders and such)
            this.formService.formWillShow(form.name, false); // false because form was already made visible server-side
            if (navigatorForm && navigatorForm.name && navigatorForm.name.lastIndexOf('default_navigator_container.html') === -1) {
                // if first show of this form in browser window then request initial data (dataproviders and such)
                this.formService.formWillShow(navigatorForm.name, false); // false because form was already made visible server-side
            }

            if (this.servoyService.getSolutionSettings().windowName === name) { // main window form switch
                    this.servoyService.getSolutionSettings().mainForm = form;
                    this.servoyService.getSolutionSettings().navigatorForm = navigatorForm;
                    if (this.appService.getUIProperty('servoy.ngclient.formbased_browser_history') !== false) {
                        this.windowRefService.nativeWindow.location.hash = form.name;
                        this.formService.goToForm(form.name);
                    }
            }
            const formCache = this.formService.getFormCacheByName(form.name);
            if (formCache) {
                formCache.navigatorForm = navigatorForm;
            }
            if (!this.windowsRestored)
            {
              this.windowsRestored = true;
              this.restoreWindows();
            }
        });
    }

   public reload() {
       window.location.reload(true);
   }

   public destroyController(formName: string) {
       this.formService.destroyFormCache(formName);
   }
}

export class SvyWindow {

    name: string;
    type: number;
    title = '';
    opacity = 1;
    undecorated = false;
    cssClassName: string = null;
    size: any = null;
    location: any = null;
    navigatorForm: any = null;
    form: any = null;
    initialBounds: any = null;
    resizable = false;
    transparent = false;
    storeBounds = false;
    bsWindowInstance: BSWindow = null;  // bootstrap-window instance , available only after creation

    windowService: WindowService;

    constructor(name: string, type: number, windowService: WindowService) {
        this.name = name;
        this.type = type;
        this.windowService = windowService;
    }

    hide() {
        if ( this.bsWindowInstance ) this.bsWindowInstance.close();
        if ( !this.storeBounds ) {
            delete this.location;
            delete this.size;
        }
        delete this.bsWindowInstance;
    }

    setLocation( location ) {
        this.location = location;
        if ( this.bsWindowInstance ) {
            this.bsWindowInstance.element.css( 'left', this.location.x + 'px' );
            this.bsWindowInstance.element.css( 'top', this.location.y + 'px' );
        }
        if ( this.storeBounds ) this.windowService.localStorageService.set(
            this.windowService.servoyService.getSolutionSettings().solutionName + this.name + '.storedBounds.location', this.location );
    }

    setSize( size ) {
        this.size = size;
        if ( this.bsWindowInstance ) {
            this.bsWindowInstance.setSize( size );
        }
        if ( this.storeBounds ) this.windowService.localStorageService.set(
            this.windowService.servoyService.getSolutionSettings().solutionName + this.name + '.storedBounds.size', this.size );
    }

    getSize() {
        return this.size;
    }

    onResize( event, size ) {
        this.size = size;
        if ( this.storeBounds ) this.windowService.localStorageService.set(
            this.windowService.servoyService.getSolutionSettings().solutionName + this.name + '.storedBounds.size', this.size );
        this.windowService.sabloService.callService( '$windowService', 'resize', { name: this.name, size: this.size }, true );
    }

    onMove( event, location ) {
        this.location = { x: location.left, y: location.top };
        if ( this.storeBounds ) this.windowService.localStorageService.set(
            this.windowService.servoyService.getSolutionSettings().solutionName + this.name + '.storedBounds.location', this.location );
        this.windowService.sabloService.callService( '$windowService', 'move', { name: this.name, location: this.location }, true );
    }

    clearBounds() {
        this.windowService.localStorageService.remove(
            this.windowService.servoyService.getSolutionSettings().solutionName + this.name + '.storedBounds.location');
        this.windowService.localStorageService.remove(
                this.windowService.servoyService.getSolutionSettings().solutionName + this.name + '.storedBounds.size');
    }
}
