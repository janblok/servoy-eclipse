import { Directive, Input, SimpleChanges, SimpleChange } from '@angular/core';
import { TestBed, async, fakeAsync, tick } from '@angular/core/testing';
import { ServoyDefaultTablesspanel } from './tablesspanel';
import { Tab } from './basetabpanel';

import { ServoyApi } from '../../ngclient/servoy_public'

import { NgbModule } from '@ng-bootstrap/ng-bootstrap';


import { WindowRefService } from '../../sablo/util/windowref.service'
import { LoggerFactory } from '../../sablo/logger.service'
import { SabloModule } from '../../sablo/sablo.module';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';

describe( 'ServoyDefaultTabpanel', () => {
    let servoyApi;
    beforeEach( () => {
        servoyApi = jasmine.createSpyObj( "ServoyApi", ["getMarkupId", "formWillShow", "hideForm"] )
        servoyApi.getMarkupId.and.returnValue( "1" );
        servoyApi.formWillShow.and.returnValue( Promise.resolve( true ) );
        servoyApi.hideForm.and.returnValue( Promise.resolve( true ) );
        TestBed.configureTestingModule( {
            declarations: [
                ServoyDefaultTablesspanel
            ],
            imports: [NgbModule, SabloModule, ServoyPublicModule, SabloModule],
            providers: [WindowRefService, LoggerFactory]
        } ).compileComponents();
    } );

    function createComponentWithTabs() {
        const fixture = TestBed.createComponent( ServoyDefaultTablesspanel );

        fixture.componentInstance.servoyApi = servoyApi as ServoyApi;
        fixture.componentInstance.visible = true;

        const tabs = [];
        let tab = new Tab();
        tab.name = "tab1";
        tab.containsFormId = "form1";
        tab.text = "tab1";
        tabs[0] = tab;
        tab = new Tab();
        tab.name = "tab2";
        tab.containsFormId = "form2";
        tab.text = "tab2";
        tabs[1] = tab;

        fixture.componentInstance.tabs = tabs;
        return fixture;
    }
    it( 'should create the tabpanel component', async(() => {
        const fixture = TestBed.createComponent( ServoyDefaultTablesspanel );
        const app = fixture.debugElement.componentInstance;
        expect( app ).toBeTruthy();
    } ) );

    it( 'should select first tab and change 2 second', fakeAsync(() => {
        const fixture = createComponentWithTabs();

        let changes: SimpleChanges = {};
        changes["tabs"] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        fixture.componentInstance.ngOnChanges( changes )

        fixture.detectChanges();
        expect( fixture.componentInstance.getSelectedTab()._id ).toBe( "1_tab_0" );
        expect( fixture.componentInstance.tabIndex ).toBe( 1 );

        fixture.componentInstance.tabIndex = 2;

        changes = {};
        changes["tabIndex"] = new SimpleChange( 1, 2, false );
        fixture.componentInstance.ngOnChanges( changes );
        tick()
        expect( fixture.componentInstance.getSelectedTab()._id).toBe( "1_tab_1" );
        expect( fixture.componentInstance.tabIndex ).toBe( 2 );
    } ) );

    it( 'should select second  tab on index', fakeAsync(() => {
        const fixture = createComponentWithTabs();
        fixture.componentInstance.tabIndex = 2;

        let changes: SimpleChanges = {};
        changes["tabs"] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        changes["tabIndex"] = new SimpleChange( null, 2, true );
        fixture.componentInstance.ngOnChanges( changes )

        fixture.detectChanges();
        expect( fixture.componentInstance.getSelectedTab()._id ).toBe( "1_tab_1" );
        expect( fixture.componentInstance.tabIndex ).toBe( 2 );
    } ) );

    it( 'should select second  tab on name', fakeAsync(() => {
        const fixture = createComponentWithTabs();
        fixture.componentInstance.tabIndex = "tab2";

        let changes: SimpleChanges = {};
        changes["tabs"] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        changes["tabIndex"] = new SimpleChange( null, 2, true );
        fixture.componentInstance.ngOnChanges( changes )

        fixture.detectChanges();
        expect( fixture.componentInstance.getSelectedTab()._id ).toBe( "1_tab_1" );
        expect( fixture.componentInstance.tabIndex ).toBe( 2 );
    } ) );

} );

