import { FormattingService, Format } from './formatting.service';
import numbro from 'numbro';
import languages from 'numbro/dist/languages.min'

describe('FormattingService', () => {
    let service: FormattingService;
    beforeEach(() => { service = new FormattingService(); });
    
    beforeAll(() => { 
       numbro.registerLanguage(languages['en-GB']); 
       numbro.registerLanguage(languages['nl-NL']); 
    });
    it('should corectly format numbers', () => {
        numbro.setLanguage("en-GB");
        var MILLSIGN = '\u2030'; //�
        var CURRENCY = '\u00A4';
        let myformat: Format = new Format();
        myformat.type = 'NUMBER';

        myformat.display = '0.000';
        expect(service.format(10.49, myformat, false)).toEqual("10.490");

        myformat.display = '0000.000';
        expect(service.format(10, myformat, false)).toEqual("0010.000");

        myformat.display = '0000.000';
        expect(service.format(-10, myformat, false)).toEqual("-0010.000");

        myformat.display = '#.###';
        expect(service.format(10.49, myformat, false)).toEqual("10.49");

        myformat.display = '#.###' + CURRENCY;
        expect(service.format(10.49, myformat, false)).toEqual("10.49\u00A3");

        myformat.display = '#.###$';
        expect(service.format(10.49, myformat, false)).toEqual("10.49$");

        myformat.display = '$ #.###';
        expect(service.format(10.49, myformat, false)).toEqual("$ 10.49");

        myformat.display = '$ -#.###';
        expect(service.format(10.49, myformat, false)).toEqual("$ 10.49");

        myformat.display = '-#.###$';
        expect(service.format(10.49, myformat, false)).toEqual("10.49$");

        myformat.display = '$ -#.###';
        expect(service.format(-10.49, myformat, false)).toEqual("$ -10.49");

        myformat.display = '-#.###$';
        expect(service.format(-10.49, myformat, false)).toEqual("-10.49$");

        myformat.display = '$ #.###-';
        expect(service.format(-10.49, myformat, false)).toEqual("$ 10.49-");

        myformat.display = '#.###-$';
        expect(service.format(-10.49, myformat, false)).toEqual("10.49-$");

        myformat.display = '€ #,##0.00;€ -#,##0.00#';
        expect(service.format(10.49, myformat, false)).toEqual("€ 10.49");

        myformat.display = '€ #,##0.00;€ -#,##0.00#';
        expect(service.format(-10.49, myformat, false)).toEqual("€ -10.49");

        myformat.display = '+#.###';
        expect(service.format(10.49, myformat, false)).toEqual("+10.49");

        myformat.display = '#,###.00';
        expect(service.format(1000, myformat, false)).toEqual("1,000.00");

        myformat.display = '#,###.##';
        expect(service.format(1000, myformat, false)).toEqual("1,000");

        myformat.display = '##-';
        expect(service.format(12, myformat, false)).toEqual("12");

        myformat.display = '##-';
        expect(service.format(-12, myformat, false)).toEqual("12-");

        myformat.display = '-##';
        expect(service.format(12, myformat, false)).toEqual("12");

        myformat.display = '-##';
        expect(service.format(-12, myformat, false)).toEqual("-12");

        myformat.display = '##.##-';
        expect(service.format(12.34, myformat, false)).toEqual("12.34");

        myformat.display = '##.##-';
        expect(service.format(-12.34, myformat, false)).toEqual("12.34-");

        myformat.display = '-##.##';
        expect(service.format(12.34, myformat, false)).toEqual("12.34");

        myformat.display = '-##.##';
        expect(service.format(-12.34, myformat, false)).toEqual("-12.34");

        myformat.display = '+0';
        //TODO numbro vs numeral difference
        expect(service.format(10.49, myformat, false)).toEqual("+10.49");

        myformat.display = '+%00.00';
        expect(service.format(10.49, myformat, false)).toEqual("+%1049.00");

        myformat.display = MILLSIGN + '+00.00';
        expect(service.format(10.49, myformat, false)).toEqual(MILLSIGN + "+10490.00");

        myformat.display = '+' + MILLSIGN + '00.00';
        expect(service.format(10.49, myformat, false)).toEqual('+' + MILLSIGN + "10490.00");

        myformat.display = '00.00E00';
        expect(service.format(10.49, myformat, false)).toEqual('1.0490e+1');

        myformat.display = '##0.0';
        expect(service.format(3.9, myformat, false)).toEqual("3.9");

        myformat.display = '##0.0';
        expect(service.format(30.9, myformat, false)).toEqual("30.9");

        myformat.display = '##0.0';
        expect(service.format(300, myformat, false)).toEqual("300.0");

        myformat.display = '000.0';
        expect(service.format(3.9, myformat, false)).toEqual("003.9");

        myformat.display = '#.#';
        expect(service.format(0.9, myformat, false)).toEqual("0.9");
    });

    it('should corectly UNformat  numbers', () => {
        numbro.setLanguage("en-GB");
        var MILLSIGN = '\u2030'; //�
        expect(service.unformat("10.49", '0.000', 'NUMBER')).toEqual(10.49);
        expect(service.unformat("+%1049.00", '+%00.00', 'NUMBER')).toEqual(10.49);
        expect(service.unformat("-10.000", '-0000.000', 'NUMBER')).toEqual(-10);
        expect(service.unformat("-10.000", '###.###', 'NUMBER')).toEqual(-10);
        expect(service.unformat("1,000", '#,###.00', 'NUMBER')).toEqual(1000);
        expect(service.unformat("1,000.00", '#,###.00', 'NUMBER')).toEqual(1000);
        expect(service.unformat("1,000.00", '#,###.##', 'NUMBER')).toEqual(1000);
        //expect(service.unformat(MILLSIGN + "+10490.00", MILLSIGN + '+00.00', 'NUMBER')).toEqual(10.49);
        expect(service.unformat('1.0490e+1', '00.00E00', 'NUMBER')).toEqual(10.49);
        expect(service.unformat("3", "0 'μm'", 'NUMBER')).toEqual(3);
    });

    it("should corectly format numbers in dutch locale", () => {
        numbro.setLanguage("nl-NL");
        const MILLSIGN = '\u2030'; //�
        let myformat: Format = new Format();
        myformat.type = 'NUMBER';

        myformat.display = '0.000';
        expect(service.format(10.49,  myformat, false)).toEqual("10,490");

        myformat.display = '0000.000';
        expect(service.format(10,  myformat, false)).toEqual("0010,000");

        myformat.display = '0000.000';
        expect(service.format(-10,  myformat, false)).toEqual("-0010,000");

        myformat.display = '#.###';
        expect(service.format(10.49,  myformat, false)).toEqual("10,49");

        myformat.display = '+#.###';
        expect(service.format(10.49,  myformat, false)).toEqual("+10,49");

        myformat.display = '#,###.00';
        expect(service.format(1000,  myformat, false)).toEqual("1.000,00");

        myformat.display = '#,###.##';
        expect(service.format(1000,  myformat, false)).toEqual("1.000");

        myformat.display = '+0';
        expect(service.format(10.49,  myformat, false)).toEqual("+10,49");

        myformat.display = '+%00.00';
        expect(service.format(10.49,  myformat, false)).toEqual("+%1049,00");

        myformat.display = MILLSIGN + '+00.00';
        expect(service.format(10.49,  myformat, false)).toEqual(MILLSIGN + "+10490,00");

        myformat.display = '+' + MILLSIGN + '00.00';
        expect(service.format(10.49,  myformat, false)).toEqual('+' + MILLSIGN + "10490,00");

        myformat.display = '00.00E00';
        expect(service.format(10.49,  myformat, false)).toEqual('1.0490e+1'); // TODO shouldn't this also be in dutch notation??
    });

    it("should corectly UNformat  numbers in dutch locale", () => {
        numbro.setLanguage("nl-NL")
        var MILLSIGN = '\u2030'; //�
        expect(service.unformat("10,49", '0.000', 'NUMBER')).toEqual(10.49);
        expect(service.unformat("+%1049,00", '+%00.00', 'NUMBER')).toEqual(10.49);
        expect(service.unformat("-10,000", '-0000.000', 'NUMBER')).toEqual(-10);
        expect(service.unformat("-10,000", '###.###', 'NUMBER')).toEqual(-10);
        expect(service.unformat("1.000", '#,###.00', 'NUMBER')).toEqual(1000);
        expect(service.unformat("1.000,00", '#,###.00', 'NUMBER')).toEqual(1000);
        expect(service.unformat("1.000,00", '#,###.##', 'NUMBER')).toEqual(1000);
        //expect(service.unformat(MILLSIGN + "+10490,00", MILLSIGN + '+00.00', 'NUMBER')).toEqual(10.49);
        expect(service.unformat('1.0490e+1', '00.00E00', 'NUMBER')).toEqual(10.49); // TODO shouldn't this also be in dutch notation??
    });
    
     it("should corectly format dates", () => {
        numbro.setLanguage("en-GB");
        var MILLSIGN = '\u2030'; //�
        // this test depends on locale, p.m. is for nl
        
        let myformat: Format = new Format();
        myformat.type = 'DATETIME';
        myformat.display = 'Z';
        const z = service.format(new Date(2014, 10, 3, 15, 23, 14), myformat, false);
        
        myformat.display = 'dd-MM-yyyy HH:mma s  G S';
        expect(service.format(new Date(2014, 10, 1, 23, 23, 14, 500), myformat, false)).toEqual("01-11-2014 23:23pm 14  AD 500");
        
        myformat.display = 'dd-MM-yyyy Z D';
        expect(service.format(new Date(2014, 10, 3, 15, 23, 14), myformat, false)).toEqual("03-11-2014 " + z + " 307"); // TODO fix timezone issues
        
        myformat.display = 'dd/MM/yyyy Z D';
        expect(service.format(new Date(2014, 10, 4, 15, 23, 14), myformat, false)).toEqual("04/11/2014 " + z + " 308"); // TODO fix timezone issues
        
        myformat.display = 'dd MM yyyy KK:mm D';
        expect(service.format(new Date(2014, 10, 5, 12, 23, 14), myformat, false)).toEqual("05 11 2014 12:23 309");
        
        myformat.display = 'dd MM yyyy kk:mm D';
        // the following sets hour to 24:23 which is next day ,so 6'th
        expect(service.format(new Date(2014, 10, 5, 24, 23, 14), myformat, false)).toEqual("06 11 2014 24:23 310");
    });
    
    it("should corectly format strings", () => {
        let myformat: Format = new Format();
        myformat.type = 'TEXT';
        
        myformat.display = 'UU##UU##';
        expect(service.format("aa11BB22", myformat,false)).toEqual("AA11BB22");
        
        myformat.display = 'HHHHUU##';
        expect(service.format("aa11BB22", myformat,false)).toEqual("AA11BB22");
        
        myformat.display = '#HHHUU##';
        expect(()=>{service.format("aa11BB22", myformat,false)}).toThrow("input string not corresponding to format : aa11BB22 , #HHHUU##");
    })
});