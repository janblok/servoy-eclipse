import { Component, ChangeDetectorRef, Renderer2, Input, SimpleChanges } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import { FormattingService } from '../../ngclient/servoy_public';

@Component({
  selector: 'servoybootstrap-textbox',
  templateUrl: './textbox.html',
  styleUrls: ['./textbox.scss']
})
export class ServoyBootstrapTextbox extends ServoyBootstrapBasefield {

  @Input() format;
  @Input() inputType;
  @Input() selectOnEnter;
  @Input() autocomplete;

  valueBeforeChange: any;
  
  constructor(renderer: Renderer2,cdRef: ChangeDetectorRef, private formattingService : FormattingService) {
    super(renderer, cdRef);
  }

  attachFocusListeners(nativeElement : any) {
    if(this.onFocusGainedMethodID)
        this.renderer.listen( nativeElement, 'focus', ( e ) => {
            this.onFocusGainedMethodID(e);
            this.valueBeforeChange = nativeElement.value;
        } );
    if(this.onFocusLostMethodID)
        this.renderer.listen( nativeElement, 'blur', ( e ) => {
            this.onFocusLostMethodID(e);
        } ); 
  }

  update(val: string) {
    if(this.format) {
        var newDataProviderID = this.formattingService.parse(val, this.format, this.dataProviderID);
        if(this.dataProviderID == newDataProviderID) {
            this.getNativeElement().value = this.valueBeforeChange;
        }
    }
    super.update(val);
  }
}
