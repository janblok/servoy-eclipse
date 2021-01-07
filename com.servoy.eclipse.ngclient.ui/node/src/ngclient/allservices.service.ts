import { NgModule } from '@angular/core';

import { Injectable } from '@angular/core';

import { ServicesService, ServiceProvider } from '../sablo/services.service';

import { ApplicationService } from './services/application.service';
import { WindowService } from './services/window.service';
import { SessionService } from './services/session.service';
import { LoadingIndicatorService } from '../sablo/util/loading-indicator/loading-indicator.service';
import { WindowService as WindowPlugin} from '../window_service/window.service';

// generated services start
import { KeyListener } from '../keylistener/keylistener.service';
import { NGUtilsService } from '../servoy_ng_only_services/ngutils/ngutils.service';
// generated services end


/**
 * this is a the all services that should also be a generated ts file.
 * This should have in providers of the @NgModule below have a list of a all the services that are in the servoy workspace/active project
 * then in the constructor it should be a list by name->class
 * so this should only have services that are called from the server side (internals like $applicationService or externals like keylistener)
 */
@Injectable()
export class AllServiceService implements ServiceProvider {
    constructor( private services: ServicesService,
        private $applicationService: ApplicationService,
        private $windowService: WindowService,
        private $sabloLoadingIndicator: LoadingIndicatorService,
        private $sessionService: SessionService,
        // generated services start
        private ngclientutils: NGUtilsService,
        private keyListener: KeyListener,
        private window: WindowPlugin
        // generated services end
    ) {
        services.setServiceProvider( this );
    }
    getService( name: string ) {
        return this[name];
    }

    init() {
        // just here is it can be called on.
    }

}

@NgModule( {
    providers: [AllServiceService, ApplicationService, WindowService, SessionService,
                // generated services start (only for dynamic services (servoy plugins))
                NGUtilsService,
                KeyListener,
                // generated services end
                ],
} )
export class AllServicesModules { }
