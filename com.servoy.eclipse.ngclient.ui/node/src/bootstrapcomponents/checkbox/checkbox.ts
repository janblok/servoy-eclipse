import { Component, OnInit, Renderer2, SimpleChanges, AfterViewInit, Input,ChangeDetectorRef } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';

@Component({
  selector: 'servoybootstrap-checkbox',
  templateUrl: './checkbox.html',
  styleUrls: ['./checkbox.scss']
})
export class ServoyBootstrapCheckbox extends ServoyBootstrapBasefield {

  @Input() showAs;

  selected: boolean = false;

  constructor(renderer: Renderer2,protected cdRef: ChangeDetectorRef) {
    super(renderer,cdRef);
  }

  svyOnInit() {
      super.svyOnInit();
      this.attachEventHandlers(this.getNativeElement())
  }

  svyOnChanges(changes: SimpleChanges) {
    for ( let property in changes ) {
        switch ( property ) {
            case "dataProviderID":
                this.setSelectionFromDataprovider()
                break;

        }
    }
    super.svyOnChanges(changes);
  }
  
  attachEventHandlers(element: any) {
    if (!element)
        element = this.getNativeElement();
    this.renderer.listen(element, 'click', (e) => {
        this.itemClicked(e);
        if (this.onActionMethodID) this.onActionMethodID(e);
    });
  }

  itemClicked(event) {
    // reverse the selected value (data provider too)
    if ( event.target.localName === 'span' || event.target.localName === 'label' 
      || event.target.localName === 'div') {
      this.selected = !this.selected;
      event.preventDefault();
    }
    if (typeof this.dataProviderID === 'string') {
      this.dataProviderID = this.dataProviderID == "1" ? "0" : "1";
    } else {
      this.dataProviderID = this.dataProviderID > 0 ? 0 : 1;
    }
    this.update(this.dataProviderID);
    event.target.blur();
  }

  setSelectionFromDataprovider() {
    this.selected = this.getSelectionFromDataprovider();
  }

  getSelectionFromDataprovider() {
    if ( !this.dataProviderID ) {
        return false;
    } else if ( typeof this.dataProviderID === "string" ) {
        return this.dataProviderID == "1";
    } else {
        return this.dataProviderID > 0;
    }
  }
}
