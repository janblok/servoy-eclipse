import { Component, Renderer2,SimpleChanges, ChangeDetectorRef} from '@angular/core';

import {FormattingService} from '../../ngclient/servoy_public'

import {ServoyDefaultBaseField} from  '../basefield'

@Component( {
    selector: 'servoydefault-imagemedia',
    templateUrl: './imagemedia.html'
} )
export class ServoyDefaultImageMedia extends ServoyDefaultBaseField{
  
    imageURL: string = "servoydefault/imagemedia/res/images/empty.gif";
    increment: number = 0;
    
    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef ,
                formattingService : FormattingService) {
        super(renderer, cdRef, formattingService);
    }
    
    deleteMedia(): void {
        this.update(null);
        this.imageURL = "servoydefault/imagemedia/res/images/empty.gif";
    }
    
    downloadMedia(): void {
        if (this.dataProviderID) {
            let x = window.screenTop + 100;
            let y = window.screenLeft + 100;
            window.open(this.dataProviderID.url ? this.dataProviderID.url : this.dataProviderID, 'download', 'top=' + x + ',left=' + y + ',screenX=' + x
                    + ',screenY=' + y + ',location=no,toolbar=no,menubar=no,width=310,height=140,resizable=yes');
        }
    }
    
    svyOnInit() {
        super.svyOnInit();
        this.updateImageURL(this.dataProviderID);
    }
    
    svyOnChanges(changes: SimpleChanges): void {
        super.svyOnChanges(changes);
        this.updateImageURL(changes.dataProviderID.currentValue);
    }
    
    private updateImageURL(dp) {
        if(dp != null && dp !='') {
            let contentType = dp.contentType;
            if (contentType != null && contentType != undefined && contentType.indexOf("image") == 0) {
                this.imageURL = dp.url;
            } else {
                this.imageURL = "servoydefault/imagemedia/res/images/notemptymedia.gif";
            }
        }
    }
}

