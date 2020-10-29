import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';

import { ServoyBootstrapTextarea } from './textarea';

describe('TextareaComponent', () => {
  let component: ServoyBootstrapTextarea;
  let fixture: ComponentFixture<ServoyBootstrapTextarea>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapTextarea ],
      imports: [FormsModule]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapTextarea);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });
});
