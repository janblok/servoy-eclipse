import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { DesignerComponent } from './designer.component';
import { ToolbarComponent } from './toolbar/toolbar.component';
import { StatusBarComponent } from './statusbar/statusbar.component';
import { PaletteComponent, SearchTextPipe, SearchTextDeepPipe } from './palette/palette.component';
import { ResizerComponent } from './resizer/resizer.component';
import { ContextMenuComponent } from './contextmenu/contextmenu.component';
import { MouseSelectionComponent } from './mouseselection/mouseselection.component';
import { HighlightComponent } from './highlight/highlight.component';
import { GhostsContainerComponent } from './ghostscontainer/ghostscontainer.component';
import { EditorContentComponent } from './editorcontent/editorcontent.component';
import {EditorSessionService} from './services/editorsession.service';
import {URLParserService} from './services/urlparser.service';
import { SabloModule } from '@servoy/sablo';
import { WindowRefService } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { NgbModule }  from '@ng-bootstrap/ng-bootstrap';
import {DragDropModule} from '@angular/cdk/drag-drop';
import { ToolbarButtonComponent } from './toolbar/item/toolbarbutton.component';
import { ToolbarSpinnerComponent } from './toolbar/item/toolbarspinner.component';
import { ToolbarSwitchComponent } from './toolbar/item/toolbarswitch.component';

@NgModule({
  declarations: [
    DesignerComponent,
    ToolbarComponent,
    ToolbarButtonComponent,
    ToolbarSpinnerComponent,
    ToolbarSwitchComponent,
    StatusBarComponent,
    PaletteComponent,
    ResizerComponent,
    ContextMenuComponent,
    MouseSelectionComponent,
    HighlightComponent,
    GhostsContainerComponent,
    EditorContentComponent,
    SearchTextPipe,
    SearchTextDeepPipe
  ],
  imports: [
    BrowserModule,
    SabloModule,
    FormsModule,
    CommonModule,
    HttpClientModule,
    NgbModule,
    DragDropModule
  ],
  providers: [EditorSessionService, URLParserService, WindowRefService],
  bootstrap: [DesignerComponent]
})
export class DesignerModule { }
