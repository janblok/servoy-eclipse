import { Component, ViewChild, SimpleChanges, Input, Renderer2, ElementRef, EventEmitter, Output, OnDestroy, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent } from '../../ngclient/servoy_public'
import { LoggerFactory, LoggerService } from '../../sablo/logger.service';
import { AngularEditorConfig } from '@kolkov/angular-editor';

@Component( {
    selector: 'servoyextra-htmlarea',
    templateUrl: './htmlarea.html',
} )
export class ServoyExtraHtmlarea extends ServoyBaseComponent {

    @Input() onActionMethodID;
    @Input() onRightClickMethodID;
    @Input() onDataChangeMethodID;
    @Input() onFocusGainedMethodID;
    @Input() onFocusLostMethodID;

    @Output() dataProviderIDChange = new EventEmitter();
    @Input() dataProviderID;
    @Input() enabled;
    @Input() editable;
    @Input() placeholderText;
    @Input() readOnly;
    @Input() responsiveHeight;
    @Input() styleClass;
    @Input() tabSeq;
    @Input() text;
    @Input() toolTipText;
    @Input() visible;

    private log: LoggerService;

    config: AngularEditorConfig = {
        editable: true,
        spellcheck: true,
        translate: 'no',
        defaultParagraphSeparator: 'p'
    };

    constructor( renderer: Renderer2, cdRef: ChangeDetectorRef, logFactory: LoggerFactory ) {
        super( renderer, cdRef );
        this.log = logFactory.getLogger( 'Htmlarea' );
    }

    svyOnInit() {
        super.svyOnInit();

        this.attachHandlers();
        this.attachFocusListeners( this.getFocusElement() );

        if ( this.dataProviderID === undefined ) {
            this.dataProviderID = null;
        }
        //        // ugly hack to fix the height
        //        let nativeElement = this.getNativeElement();
        //        let componentHeight = nativeElement.offsetHeight;
        //        //let toolBarHeight = nativeElement.childNodes[0].childNodes[0].childNodes[1].childNodes[1].offsetHeight;
        //        let initialContentHeight = nativeElement.childNodes[0].childNodes[0].childNodes[2].childNodes[0].offsetHeight;
        //        let initialEditorHeight = nativeElement.childNodes[0].childNodes[0].offsetHeight;
        //        
        //        this.renderer.setStyle( nativeElement.childNodes[0].childNodes[0].childNodes[2].childNodes[0], "height",  (initialContentHeight + componentHeight - initialEditorHeight) +'px');

    }

    svyOnChanges( changes: SimpleChanges ) {
        if ( changes ) {
            for ( const property of Object.keys( changes ) ) {
                const change = changes[property];
                switch ( property ) {
                    case 'enabled':
                        if ( change.currentValue )
                            this.renderer.removeAttribute( this.getFocusElement(), 'disabled' );
                        else
                            this.renderer.setAttribute( this.getFocusElement(), 'disabled', 'disabled' );
                        break;
                    case 'editable':
                        if ( change.currentValue )
                            this.renderer.removeAttribute( this.getFocusElement(), 'readonly' );
                        else
                            this.renderer.setAttribute( this.getFocusElement(), 'readonly', 'readonly' );
                        break;
                    case 'placeholderText':
                        if ( change.currentValue ) this.renderer.setAttribute( this.getFocusElement(), 'placeholder', change.currentValue );
                        else this.renderer.removeAttribute( this.getFocusElement(), 'placeholder' );
                        break;
                    case 'styleClass':
                        if ( change.previousValue )
                            this.renderer.removeClass( this.getNativeElement(), change.previousValue );
                        if ( change.currentValue )
                            this.renderer.addClass( this.getNativeElement(), change.currentValue );
                        break;
                }
            }
        }
        super.svyOnChanges( changes );
    }

    getFocusElement(): any {
        return this.getNativeElement();
    }

    pushUpdate() {
        this.dataProviderIDChange.emit( this.dataProviderID );
    }

    attachFocusListeners( nativeElement: any ) {
        if ( this.onFocusGainedMethodID )
            this.renderer.listen( nativeElement, 'focus', ( e ) => {
                this.onFocusGainedMethodID( e );
            } );
        if ( this.onFocusLostMethodID )
            this.renderer.listen( nativeElement, 'blur', ( e ) => {
                this.onFocusLostMethodID( e );
            } );
    }

    protected attachHandlers() {
        if ( this.onActionMethodID ) {

            if ( this.getNativeElement().tagName == 'TEXTAREA' || this.getNativeElement().type == 'text' ) {
                this.renderer.listen( this.getNativeElement(), 'keydown', e => { if ( e.keyCode == 13 ) this.onActionMethodID( e ) } );
            }
            else {
                this.renderer.listen( this.getNativeElement(), 'click', e => this.onActionMethodID( e ) );
            }
        }
        if ( this.onRightClickMethodID ) {
            this.renderer.listen( this.getNativeElement(), 'contextmenu', e => { this.onRightClickMethodID( e ); return false; } );
        }
    }
}
