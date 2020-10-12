import { Directive , Input , ElementRef, HostListener} from '@angular/core';
import { Format } from '../format/format.directive';
import { getLocaleNumberSymbol, NumberSymbol } from '@angular/common';
import { ServoyService } from '../servoy.service';

@Directive({
  selector: '[svyDecimalKeyConverter]'
})
export class DecimalkeyconverterDirective {

  @Input('svyDecimalKeyConverter') svyFormat: Format;
  private element: HTMLInputElement;

  public constructor(private el: ElementRef) {
      this.element = el.nativeElement;
  }

  @HostListener('keydown', ['$event']) onKeypress(e: KeyboardEvent) {
      if (e.which === 110 && this.svyFormat && this.svyFormat.type === 'NUMBER') {
          const caretPos = this.element.selectionStart;
          const startString = this.element.value.slice(0, caretPos);
          const endString = this.element.value.slice(this.element.selectionEnd, this.element.value.length);
          this.element.value = (startString + getLocaleNumberSymbol(ServoyService.LOCALE, NumberSymbol.Decimal) + endString);
          this.element.focus();
          this.element.setSelectionRange(caretPos + 1, caretPos + 1);
          if (e.preventDefault) e.preventDefault();
      }
  }
}
