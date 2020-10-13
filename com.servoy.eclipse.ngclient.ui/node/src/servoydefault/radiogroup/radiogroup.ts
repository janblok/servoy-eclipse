import {Component, OnInit, Renderer2, ChangeDetectorRef} from '@angular/core';
import {ServoyDefaultBaseChoice} from "../basechoice";
import {FormattingService, PropertyUtils} from "../../ngclient/servoy_public";

@Component({
  selector: 'servoydefault-radiogroup',
  templateUrl: './radiogroup.html',
  styleUrls: ['./radiogroup.css']
})
export class ServoyDefaultRadiogroup extends ServoyDefaultBaseChoice{

  value: any;
  constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, formattingService: FormattingService) {
    super(renderer, cdRef, formattingService);
  }

  setSelectionFromDataprovider(){
    this.value = this.dataProviderID;
    if (this.valuelistID)
    {
      for(let i=0;i < this.valuelistID.length;i++){
        let item= this.valuelistID[i];
        if((item.realValue+'') === (this.dataProviderID+''))
        {
          this.value = item.realValue;
          break;
        }
      }
    }
  }

  itemClicked (event,index) {
    let newValue = event.target.value ? event.target.value : event.target.innerText;
    let changed = !(newValue === this.value);
    this.value = newValue;

    super.baseItemClicked(event, changed, newValue);
  }

  attachEventHandlers(element, index){
    this.renderer.listen( element, 'click', ( e ) => {
      this.itemClicked(e,index);
      if (this.onActionMethodID) this.onActionMethodID( e );
    });
    super.attachEventHandlers(element,index);
  }

}
