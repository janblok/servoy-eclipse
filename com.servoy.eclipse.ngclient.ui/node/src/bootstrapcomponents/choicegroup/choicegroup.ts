import { Component, OnInit, Input, Renderer2, SimpleChanges } from '@angular/core';
import { IValuelist } from '../../sablo/spectypes.service';
import { ServoyBootstrapBasefield } from '../bts_basefield';

@Component({
  selector: 'servoybootstrap-choicegroup',
  templateUrl: './choicegroup.html',
  styleUrls: ['./choicegroup.scss']
})
export class ServoyBootstrapChoicegroup extends ServoyBootstrapBasefield implements OnInit {

  @Input() inputType;
  @Input() findmode;
  @Input() valuelistID : IValuelist;
  @Input() showAs;
  selection: any[] = [];
  allowNullinc = 0;

  constructor(renderer: Renderer2) {
    super(renderer);
  }

  ngOnInit(){
    this.onValuelistChange();
  }

  ngAfterViewInit(){
      this.setHandlersAndTabIndex();
  }

  ngOnChanges( changes: SimpleChanges ) {
    for ( let property in changes ) {
        switch ( property ) {
            case "dataProviderID":
                this.setSelectionFromDataprovider()
                break;

        }
    }
    super.ngOnChanges(changes);
  }

  setHandlersAndTabIndex() {
    for(let i = 0; i < this.getNativeElement().children.length; i++){
        let elm:HTMLLabelElement = this.getNativeElement().children[i];
        this.attachEventHandlers(elm.children[0],i);
      }     
  }

  onValuelistChange() {
    if(this.valuelistID)
        if(this.valuelistID.length > 0 && this.isValueListNull(this.valuelistID[0])) this.allowNullinc=1;
    this.setHandlersAndTabIndex();
  };

  getDataproviderFromSelection() {
    let returnValue = "";
    this.selection.forEach((element, index) => {
      if (element === true)
        returnValue += this.valuelistID[index + this.allowNullinc].realValue + '\n' 
    });
    returnValue.replace(/\n$/, ''); // remove the last \n
    if (returnValue === '') returnValue = null;
    return returnValue;
  }

  setSelectionFromDataprovider() {
    this.selection = [];
    if (this.dataProviderID === null || this.dataProviderID === undefined) return;
    const arr = (typeof this.dataProviderID === 'string') ? this.dataProviderID.split('\n') : [this.dataProviderID];
    arr.forEach( (element, index, array) => {
      for (let i = 0; i < this.valuelistID.length; i++) {
        const item = this.valuelistID[i];
        if (item.realValue + '' === element + '' && !this.isValueListNull(item)) {
          if (this.inputType === 'radio') {
            if (arr.length > 1) this.selection = []; else this.selection[i-this.allowNullinc] = item.realValue;
          } else {
            this.selection[i - this.allowNullinc] = true;
          }
        }
      }
    });
  }

  isValueListNull = function(item) {
    return (item.realValue == null || item.realValue == '') && item.displayValue == '';
  };

  itemClicked(event, index) {
    let changed = true;
    if (this.inputType === 'radio') {
      this.dataProviderID = this.valuelistID[index + this.allowNullinc].realValue;
    } else {
      let checkedTotal = 0;
      for (let i = 0; i < this.selection.length; i++) {
        if (this.selection[i] == true) checkedTotal++;
      }
      changed = !(checkedTotal == 0 && this.allowNullinc == 0 && !this.findmode);
      if (!changed) {
        this.selection[index] = true;
      }
      this.dataProviderID = this.getDataproviderFromSelection();
    }
    if (changed) this.update(this.dataProviderID);
    event.target.blur();
  }

  attachEventHandlers(element, index) {
    if (element) {
      this.renderer.listen( element, 'click', ( event ) => {
        this.itemClicked(event, index);
        if (this.onActionMethodID) this.onActionMethodID( event );
      });
      this.attachFocusListeners(element);
    }
  }
}
