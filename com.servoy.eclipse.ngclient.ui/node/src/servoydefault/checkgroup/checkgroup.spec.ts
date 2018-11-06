import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import {ServoyDefaultCheckGroup} from './checkgroup';
import {SabloModule} from "../../sablo/sablo.module";
import {
  DecimalkeyconverterDirective,
  FormatFilterPipe,
  FormattingService, StartEditDirective,ServoyApi,
  SvyFormat
} from "../../ngclient/servoy_public";
import {FormsModule} from "@angular/forms";
import {NotNullOrEmptyPipe} from "../../ngclient/pipes/pipes";
import { of } from 'rxjs';

const mockData = [
                          {
                            realValue: 3,
                            displayValue: "Bucharest"
                          },
                          {
                            realValue: 1,
                            displayValue: "Timisoara"
                          },
                          {
                            realValue: 2,
                            displayValue: "Amsterdam"
                          },
                        ];

describe('ServoyDefaultCheckGroup', () => {
  let component: ServoyDefaultCheckGroup;
  let fixture: ComponentFixture<ServoyDefaultCheckGroup>;
  let servoyApi;

  beforeEach(async(() => {
      servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml","getApiData", "getDataProviderID"]);
      servoyApi.getApiData.and.returnValue( mockData );
      servoyApi.getDataProviderID.and.returnValue( of(1) );
      
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultCheckGroup,NotNullOrEmptyPipe, SvyFormat,DecimalkeyconverterDirective, StartEditDirective ],
      imports: [SabloModule, FormsModule],
      providers: [NotNullOrEmptyPipe,FormattingService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
      fixture = TestBed.createComponent(ServoyDefaultCheckGroup);
      fixture.componentInstance.servoyApi = servoyApi as ServoyApi;
      component = fixture.componentInstance;

      fixture.detectChanges();
  });

  it('should create component', () => {
      expect(component).toBeTruthy();
    });

});


/*
* 	//Test onFocusGained/Lost on editable checkbox and radio, case SVY-8434
	element.all(by.xpath("//*[@data-svy-name='main.check']/div/label/input")).click();
	expect(element(by.xpath("//*[@data-svy-name='main.lbl_focus']")).getText()).toBe('focusoncheck');
	expect(element(by.xpath("//*[@data-svy-name='main.lbl_focus2']")).getText()).toBe('focuslostoncheck');
	element.all(by.xpath("//*[@data-svy-name='main.radio']/div/label/input")).click();
	expect(element(by.xpath("//*[@data-svy-name='main.lbl_focus']")).getText()).toBe('focusonradio');
	expect(element(by.xpath("//*[@data-svy-name='main.lbl_focus2']")).getText()).toBe('focuslostonradio');



*
*
* */
