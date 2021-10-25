import { Component, OnInit, AfterViewInit, Inject, ViewChild, ElementRef, Renderer2, QueryList, ViewChildren, OnDestroy, Directive, Input } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { EditorSessionService, ISelectionChangedListener } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';
import { DesignerUtilsService } from '../services/designerutils.service';

@Component({
    selector: 'selection-decorators',
    templateUrl: './mouseselection.component.html',
    styleUrls: ['./mouseselection.component.css']
})
// this should include lasso and all selection logic from mouseselection.js and dragselection.js
export class MouseSelectionComponent implements OnInit, AfterViewInit, ISelectionChangedListener, OnDestroy {

    @ViewChild('lasso', { static: false }) lassoRef: ElementRef;
    @ViewChildren('selected') selectedRef: QueryList<ElementRef>;

    nodes: Array<SelectionNode> = new Array<SelectionNode>();
    contentInit: boolean = false;
    topAdjust: number;
    leftAdjust: number;
    contentRect: DOMRect;
    lassostarted: boolean = false;

    mousedownpoint: Point;
    selectedRefSubscription: any;
    editorStateSubscription: any;
    removeSelectionChangedListener: () => void;

    constructor(public readonly editorSession: EditorSessionService, @Inject(DOCUMENT) private doc: Document, protected readonly renderer: Renderer2,
        protected urlParser: URLParserService, protected designerUtilsService: DesignerUtilsService) {
        this.removeSelectionChangedListener = this.editorSession.addSelectionChangedListener(this);
    }

    ngOnInit(): void {
        this.editorSession.requestSelection();
        let content = this.doc.querySelector('.content-area') as HTMLElement;
        content.addEventListener('mousedown', (event) => this.onMouseDown(event));
        content.addEventListener('mouseup', (event) => this.onMouseUp(event));
        content.addEventListener('mousemove', (event) => this.onMouseMove(event));
    }

    ngOnDestroy(): void {
        if (this.selectedRefSubscription !== undefined) this.selectedRefSubscription.unsubscribe();
        this.editorStateSubscription.unsubscribe();
        this.removeSelectionChangedListener();
    }

    ngAfterViewInit(): void {
        this.doc.querySelector('iframe').addEventListener('load', () => {
            this.contentInit = true;
            this.calculateAdjustToMainRelativeLocation();
            this.createNodes(this.editorSession.getSelection())
        });

        this.editorStateSubscription = this.editorSession.stateListener.subscribe(id => {
            if (id === 'showWireframe') {
                Array.from(this.selectedRef).forEach((selectedNode) => {
                    if (!this.editorSession.getState().showWireframe) {
                        this.renderer.removeClass(selectedNode.nativeElement, 'showWireframe');
                    }
                });

                setTimeout(() => {
                    this.redrawDecorators();
                    if (this.editorSession.getState().showWireframe) {
                        this.applyWireframe();
                    }
                }, 400);//TODO can we do this without timeouts? how do we know the iframe content is there?
            }
        });
    }
    redrawDecorators() {
        if (this.nodes.length > 0) {
            const iframe = this.doc.querySelector('iframe');
            Array.from(this.nodes).forEach(selected => {
                let node = iframe.contentWindow.document.querySelectorAll('[svy-id="' + selected.svyid + '"]')[0];
                if (node === undefined) return;
                const position = node.getBoundingClientRect();
                selected.style = {
                    height: position.height + 'px',
                    width: position.width + 'px',
                    top: position.top + this.topAdjust + 'px',
                    left: position.left + this.leftAdjust + 'px',
                    display: 'block'
                };
            });
        }
    }

    selectionChanged(selection: Array<string>): void {
        if (this.contentInit) {
            this.createNodes(selection);
        }
    }

    private createNodes(selection: Array<string>) {
        let newNodes = new Array<SelectionNode>();
        if (selection.length > 0) {
            let iframe = this.doc.querySelector('iframe');
            let elements = iframe.contentWindow.document.querySelectorAll('[svy-id]');
            if (elements.length == 0) {
                setTimeout(() => this.createNodes(selection), 400);
                return;
            }
            Array.from(elements).forEach(node => {
                if (selection.indexOf(node.getAttribute('svy-id')) >= 0) {
                    let position = node.getBoundingClientRect();
                    this.designerUtilsService.adjustElementRect(node, position);
                    let style = {
                        height: position.height + 'px',
                        width: position.width + 'px',
                        top: position.top + this.topAdjust + 'px',
                        left: position.left + this.leftAdjust + 'px',
                        display: 'block'
                    };

                    newNodes.push({
                        style: style,
                        isResizable: this.urlParser.isAbsoluteFormLayout() ? { t: true, l: true, b: true, r: true } : { t: false, l: false, b: false, r: false },
                        svyid: node.getAttribute('svy-id'),
                        isContainer: node.getAttribute('svy-layoutname') != null,
                        maxLevelDesign: node.classList.contains('maxLevelDesign')
                    })
                }
            });
        }

        this.nodes = newNodes;
    }

    private calculateAdjustToMainRelativeLocation() {
        if (!this.topAdjust) {
            let content = this.doc.querySelector('.content-area') as HTMLElement;
            this.contentRect = content.getBoundingClientRect()
            let computedStyle = window.getComputedStyle(content, null)
            this.topAdjust = parseInt(computedStyle.getPropertyValue('padding-left').replace("px", ""));
            this.leftAdjust = parseInt(computedStyle.getPropertyValue('padding-top').replace("px", ""))
        }
    }

    private onMouseDown(event: MouseEvent) {
        if (this.editorSession.getState().dragging) return;
        this.lassostarted = false;
        const point = { x: event.pageX, y: event.pageY };
        const frameElem = this.doc.querySelector('iframe');
        const frameRect = frameElem.getBoundingClientRect();
        point.x = point.x - frameRect.left;
        point.y = point.y - frameRect.top;
        this.calculateAdjustToMainRelativeLocation();

        const elements = frameElem.contentWindow.document.querySelectorAll('[svy-id]');
        let found = Array.from(elements).reverse().find((node) => {
            const position = node.getBoundingClientRect();
            this.designerUtilsService.adjustElementRect(node, position);
            let addToSelection = false;
            if (node['offsetParent'] !== null && position.x <= point.x && position.x + position.width >= point.x && position.y <= point.y && position.y + position.height >= point.y) {
                addToSelection = true;
            }
            else if (node['offsetParent'] !== null && parseInt(window.getComputedStyle(node, ":before").height) > 0) {
                const computedStyle = window.getComputedStyle(node, ":before");
                //the top and left positions of the before pseudo element are computed as the sum of:
                //top/left position of the element, padding Top/Left of the element and margin Top/Left of the pseudo element
                const top = position.top + parseInt(window.getComputedStyle(node).paddingTop) + parseInt(computedStyle.marginTop);
                const left = position.left + parseInt(window.getComputedStyle(node).paddingLeft) + parseInt(computedStyle.marginLeft);
                const height = parseInt(computedStyle.height);
                const width = parseInt(computedStyle.width);
                if (point.y >= top && point.x >= left && point.y <= top + height && point.x <= left + width) {
                    addToSelection = true;
                }
            }

            if (addToSelection) {
                const id = node.getAttribute('svy-id');
                let selection = this.editorSession.getSelection();
                const newNode = {
                    style: {
                        height: position.height + 'px',
                        width: position.width + 'px',
                        top: position.top + this.topAdjust + 'px',
                        left: position.left + this.leftAdjust + 'px',
                        display: 'block'
                    },
                    isResizable: this.urlParser.isAbsoluteFormLayout() ? { t: true, l: true, b: true, r: true } : { t: false, l: false, b: false, r: false },
                    svyid: node.getAttribute('svy-id'),
                    isContainer: node.getAttribute('svy-layoutname') != null,
                    maxLevelDesign: node.classList.contains('maxLevelDesign')
                };
                if (event.ctrlKey || event.metaKey) {
                    let index = selection.indexOf(id);
                    if (index >= 0) {
                        this.nodes.splice(index, 1);
                        selection.splice(index, 1);
                    }
                    else {
                        this.nodes.push(newNode);
                        selection.push(id);
                    }
                }
                else {
                    let newNodes = new Array<SelectionNode>();
                    newNodes.push(newNode);
                    this.nodes = newNodes;
                    selection = [id];
                }
                this.editorSession.setSelection(selection, this);
                this.selectedRefSubscription = this.selectedRef.changes.subscribe(() => {
                    this.applyWireframe();
                })
                return node;
            }
        });
        if (!found) {
            this.nodes = [];
            this.editorSession.setSelection([], this);

            this.renderer.setStyle(this.lassoRef.nativeElement, 'left', event.pageX - this.contentRect.left + 'px');
            this.renderer.setStyle(this.lassoRef.nativeElement, 'top', event.pageY - this.contentRect.top + 'px');
            this.renderer.setStyle(this.lassoRef.nativeElement, 'width', '0px');
            this.renderer.setStyle(this.lassoRef.nativeElement, 'height', '0px');

            this.lassostarted = true;
            this.mousedownpoint = { x: event.pageX, y: event.pageY };
        }
    }

    private onMouseUp(event: MouseEvent) {
        if (this.editorSession.getState().dragging) return;
        if (this.lassostarted && this.mousedownpoint.x != event.pageX && this.mousedownpoint.y != event.pageY) {
            const frameElem = this.doc.querySelector('iframe');
            const frameRect = frameElem.getBoundingClientRect();
            const elements = frameElem.contentWindow.document.querySelectorAll('[svy-id]');
            const newNodes = new Array<SelectionNode>();
            const newSelection = new Array<string>();
            Array.from(elements).forEach((node) => {
                let position = node.getBoundingClientRect();
                this.designerUtilsService.adjustElementRect(node, position);
                if (this.rectangleContainsPoint({ x: event.pageX, y: event.pageY }, { x: this.mousedownpoint.x, y: this.mousedownpoint.y }, { x: position.x + frameRect.x, y: position.y + frameRect.y }) ||
                    this.rectangleContainsPoint({ x: event.pageX, y: event.pageY }, { x: this.mousedownpoint.x, y: this.mousedownpoint.y }, { x: position.x + frameRect.x + position.width, y: position.y + frameRect.y }) ||
                    this.rectangleContainsPoint({ x: event.pageX, y: event.pageY }, { x: this.mousedownpoint.x, y: this.mousedownpoint.y }, { x: position.x + frameRect.x, y: position.y + frameRect.y + position.height }) ||
                    this.rectangleContainsPoint({ x: event.pageX, y: event.pageY }, { x: this.mousedownpoint.x, y: this.mousedownpoint.y }, { x: position.x + frameRect.x + position.width, y: position.y + frameRect.y + position.height })) {
                    let newNode: SelectionNode = {
                        style: {
                            height: position.height + 'px',
                            width: position.width + 'px',
                            top: position.top + this.topAdjust + 'px',
                            left: position.left + this.leftAdjust + 'px',
                            display: 'block'
                        },
                        svyid: node.getAttribute('svy-id'),
                        isContainer: node.getAttribute('svy-layoutname') != null,
                        maxLevelDesign: node.classList.contains('maxLevelDesign')
                    };
                    newNodes.push(newNode);
                    newSelection.push(node.getAttribute('svy-id'))
                }
            });
            this.nodes = newNodes;
            this.editorSession.setSelection(newSelection, this);
        }
        this.lassostarted = false;
        this.applyWireframe();
    }

    private applyWireframe() {
        Array.from(this.selectedRef).forEach((selectedNode) => {
            this.applyWireframeForNode(selectedNode);
        });
    }
    applyWireframeForNode(selectedNode: ElementRef<any>) {
        const iframe = this.doc.querySelector('iframe');
        const node = iframe.contentWindow.document.querySelectorAll('[svy-id="' + selectedNode.nativeElement.getAttribute('id') + '"]')[0];
        if (node === undefined) return;
        const position = node.getBoundingClientRect();
        // TODO is && node.getAttribute('svy-layoutname') needed??
        if (node.classList.contains('svy-layoutcontainer') && !node.getAttribute('data-maincontainer') && position.width > 0 && position.height > 0) {
            this.renderer.setAttribute(selectedNode.nativeElement, 'svytitle', node.getAttribute('svy-title'));
            if (this.editorSession.getState().showWireframe) {
                this.renderer.addClass(selectedNode.nativeElement, 'showWireframe');
            }
            selectedNode.nativeElement.style.setProperty('--svyBackgroundColor', window.getComputedStyle(node).backgroundColor);
            if (node.classList.contains('maxLevelDesign')) {
                //fix for IE container background, the one above is still needed for the ::before pseudoelement
                selectedNode.nativeElement.style.setProperty('backgroundColor', window.getComputedStyle(node).backgroundColor);
                this.renderer.addClass(selectedNode.nativeElement, 'maxLevelDesign');
            }
        }
    }

    private onMouseMove(event: MouseEvent) {
        if (this.editorSession.getState().dragging) return;
        if (this.lassostarted) {
            if (event.pageX < this.mousedownpoint.x) {
                this.renderer.setStyle(this.lassoRef.nativeElement, 'left', event.pageX - this.contentRect.left + 'px');
            }
            if (event.pageY < this.mousedownpoint.y) {
                this.renderer.setStyle(this.lassoRef.nativeElement, 'top', event.pageY - this.contentRect.top + 'px');
            }
            const currentWidth = event.pageX - this.mousedownpoint.x;
            const currentHeight = event.pageY - this.mousedownpoint.y;
            this.renderer.setStyle(this.lassoRef.nativeElement, 'width', Math.abs(currentWidth) + 'px');
            this.renderer.setStyle(this.lassoRef.nativeElement, 'height', Math.abs(currentHeight) + 'px');
        }
    }

    private rectangleContainsPoint(p1: Point, p2: Point, toCheck: Point): boolean {
        if (p1.x > p2.x) {
            let temp = p1.x;
            p1.x = p2.x;
            p2.x = temp;
        }
        if (p1.y > p2.y) {
            let temp = p1.y;
            p1.y = p2.y;
            p2.y = temp;
        }
        if (p1.x <= toCheck.x && p2.x >= toCheck.x && p1.y <= toCheck.y && p2.y >= toCheck.y) {
            return true;
        }
        return false;
    }

    deleteAction(event: MouseEvent) {
        event.stopPropagation();
        this.editorSession.keyPressed({ "keyCode": 46 });
    }

    zoomInAction(event: MouseEvent) {
        event.stopPropagation();
        this.editorSession.executeAction("zoomIn");
    }

    copyAction(event: MouseEvent) {
        event.stopPropagation();
        this.editorSession.executeAction("copy");
    }

    insertACopyAction(event: MouseEvent, node: SelectionNode, before: boolean) {
        event.stopPropagation();
        const component = {}
        const frameElem = this.doc.querySelector('iframe');
        const htmlNode = frameElem.contentWindow.document.querySelector('[svy-id="' + node.svyid + '"]');

        const layoutPackage = htmlNode.getAttribute("svy-layoutname").split(".");
        component['packageName'] = layoutPackage[0];
        component['name'] = layoutPackage[1];
        var droptarget = (htmlNode.parentNode as HTMLElement).getAttribute("svy-id");
        if (droptarget) component['dropTargetUUID'] = droptarget;
        if (before) {
            component['rightSibling'] = node.svyid;
        }
        else
            if (htmlNode.nextElementSibling) {
                component['rightSibling'] = htmlNode.nextElementSibling.getAttribute("svy-id");
            }

        component['keepOldSelection'] = true;
        this.editorSession.createComponent(component);
    }

    onEnter(event: MouseEvent) {
        ((event.srcElement as Node).nextSibling as HTMLElement).style.display = 'block'
    }

    onLeave(event: MouseEvent) {
        (event.srcElement as HTMLElement).style.display = 'none'
    }

}
@Directive({
    selector: '[positionMenu]'
})
export class PositionMenuDirective implements OnInit {
    @Input('positionMenu') selectionNode: SelectionNode;
    
    constructor(@Inject(DOCUMENT) private doc: Document,  private elementRef: ElementRef){
        
    }
     
    ngOnInit(): void {
        const frameElem = this.doc.querySelector('iframe');
        const htmlNode = frameElem.contentWindow.document.querySelector('[svy-id="' + this.selectionNode.svyid + '"]');
        if (parseInt(window.getComputedStyle(htmlNode, ":before").height) > 0) {
            const computedStyle = window.getComputedStyle(htmlNode, ":before");
            const left = parseInt( window.getComputedStyle(htmlNode, null).getPropertyValue('padding-left')) + parseInt(computedStyle.marginLeft);
            const right = htmlNode.getBoundingClientRect().width - left - parseInt(computedStyle.width);
            this.elementRef.nativeElement.style.marginRight  =  right + "px";
        }
    }
}

export class SelectionNode {
    svyid: string;
    style: any;
    isResizable?: ResizeDefinition;
    isContainer: boolean;
    maxLevelDesign: boolean;
}
export class Point {
    x: number;
    y: number;
}
class ResizeDefinition {
    t: boolean;
    l: boolean;
    b: boolean;
    r: boolean;
}
