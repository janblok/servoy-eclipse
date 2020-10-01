import { async, ComponentFixture, TestBed, tick, fakeAsync, flushMicrotasks } from '@angular/core/testing';
import { ServoyDefaultCombobox } from './combobox';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { FormattingService, ServoyApi, TooltipService } from '../../ngclient/servoy_public';
import { SabloModule } from '../../sablo/sablo.module';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { of } from 'rxjs';

import { ConverterService } from '../../sablo/converter.service';
import { SabloService } from '../../sablo/sablo.service';
import { SabloDeferHelper } from '../../sablo/defer.service';
import { ValuelistConverter } from '../../ngclient/converters/valuelist_converter';
import { SpecTypesService } from '../../sablo/spectypes.service';
import { LoadingIndicatorService } from '../../sablo/util/loading-indicator/loading-indicator.service';
import { SessionStorageService } from '../../ngclient/services/webstorage/sessionstorage.service';
import { ServicesService } from '../../sablo/services.service';
import { WebsocketService } from '../../sablo/websocket.service';
import { WindowRefService } from '../../sablo/util/windowref.service';
import { LoggerFactory } from '../../sablo/logger.service';
import { Select2Data } from 'ng-select2-component';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

const mockData = [
  {
    realValue: 1,
    displayValue: 'Bucuresti'
  },
  {
    realValue: 2,
    displayValue: 'Timisoara'
  },
  {
    realValue: 3,
    displayValue: 'Cluj'
  },
]

const formattedData : Select2Data = [
    { value: '1', label: 'Bucuresti' },
    { value: '2', label: 'Timisoara'},
    { value: '3', label: 'Cluj' }
]

function createDefaultValuelist() {
  const json = {};
  json['values'] = mockData;
  json['valuelistid'] = 1073741880;
  return json;
}

describe('ComboboxComponent', () => {
  let component: ServoyDefaultCombobox;
  let fixture: ComponentFixture<ServoyDefaultCombobox>;
  let servoyApi;
  let combobox: DebugElement;
  let converterService: ConverterService;

  beforeEach(async(() => {
    servoyApi = jasmine.createSpyObj( 'ServoyApi', ['getMarkupId', 'trustAsHtml']);


    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultCombobox],
      providers: [ FormattingService, TooltipService, ValuelistConverter, ConverterService, SabloService, SabloDeferHelper, SpecTypesService,
        LoggerFactory, WindowRefService, WebsocketService, ServicesService, SessionStorageService, LoadingIndicatorService],
      imports: [ServoyPublicModule, SabloModule, NgbModule]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    const sabloService: SabloService = TestBed.get( SabloService );
    const sabloDeferHelper = TestBed.get( SabloDeferHelper );
    converterService = TestBed.get( ConverterService );
    converterService.registerCustomPropertyHandler( 'valuelist', new ValuelistConverter( sabloService, sabloDeferHelper) );

    fixture = TestBed.createComponent(ServoyDefaultCombobox);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;
    combobox = fixture.debugElement.query(By.css('select2'));

    component = fixture.componentInstance;
    component.valuelistID = converterService.convertFromServerToClient(createDefaultValuelist(),'valuelist');
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml', 'startEdit']);
    component.dataProviderID = 3;
    component.ngOnInit();

    fixture.detectChanges();
  }); 

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial length = 3', () => {
    expect(component.valuelistID.length).toBe(3);
  });

  it('should have the default value 3', () => {
    component.observableValue.subscribe(value => {
      expect(value).toBe(3); // data provider's value
    });
  });

  it('should have called servoyApi.getMarkupId', () => {
    expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
  });

  it('should use start edit directive', () => {
    combobox.triggerEventHandler('focus', null);
    fixture.detectChanges();
    expect(component.servoyApi.startEdit).toHaveBeenCalled();
  });

  it('should call update method', () => {
    spyOn(component, 'updateValue');
    combobox.nativeElement.dispatchEvent(new Event('update')); 
    fixture.detectChanges();
    expect(component.updateValue).toHaveBeenCalled();
  });

});
