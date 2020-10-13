import { Component, Renderer2, AfterViewInit, ViewChild, ElementRef, SimpleChanges, ChangeDetectorRef } from '@angular/core';

import { ServoyBootstrapBaseLabel } from '../bts_baselabel';
 
@Component( {
    selector: 'servoybootstrap-button',
    templateUrl: './button.html',
    styleUrls: ['./button.scss']
} )
export class ServoyBootstrapButton extends ServoyBootstrapBaseLabel {

    constructor(renderer: Renderer2, protected cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    svyOnInit() {
        super.svyOnInit();
        if ( this.onDoubleClickMethodID ) {
            this.renderer.listen( this.elementRef.nativeElement, 'dblclick', ( e ) => {
                this.onDoubleClickMethodID( e );
            } );
        }
    }
}

