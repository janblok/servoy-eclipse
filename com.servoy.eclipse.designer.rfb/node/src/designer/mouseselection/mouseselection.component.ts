import { Component, OnInit, AfterViewInit, Inject, ViewChild, ElementRef, Renderer2, QueryList, ViewChildren, OnDestroy, Directive, Input } from '@angular/core';
import { EditorSessionService, ISelectionChangedListener } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';
import { DesignerUtilsService } from '../services/designerutils.service';
import { Subscription } from 'rxjs';
import { EditorContentService, IContentMessageListener } from '../services/editorcontent.service';

@Component({
    selector: 'selection-decorators',
    templateUrl: './mouseselection.component.html',
    styleUrls: ['./mouseselection.component.css']
})
// this should include lasso and all selection logic from mouseselection.js and dragselection.js
export class MouseSelectionComponent implements OnInit, AfterViewInit, ISelectionChangedListener, OnDestroy, IContentMessageListener {

    @ViewChild('lasso', { static: false }) lassoRef: ElementRef<HTMLElement>;
    @ViewChildren('selected') selectedRef: QueryList<ElementRef<HTMLElement>>;

    nodes: Array<SelectionNode> = new Array<SelectionNode>();
    contentInit = false;
    topAdjust: number;
    leftAdjust: number;
    contentRect: DOMRect;
    lassostarted = false;

    mousedownpoint: Point;
    fieldLocation: Point;
    selectedRefSubscription: Subscription;
    editorStateSubscription: Subscription;
    removeSelectionChangedListener: () => void;

    constructor(public readonly editorSession: EditorSessionService, protected readonly renderer: Renderer2,
        protected urlParser: URLParserService, protected designerUtilsService: DesignerUtilsService,
        private editorContentService: EditorContentService) {
        this.editorContentService.addContentMessageListener(this);
        this.removeSelectionChangedListener = this.editorSession.addSelectionChangedListener(this);
    }

    ngOnInit(): void {
        void this.editorSession.requestSelection();
        this.editorContentService.getGlassPane().addEventListener('mousedown', (event) => this.onMouseDown(event));
        this.editorContentService.getGlassPane().addEventListener('mouseup', (event) => this.onMouseUp(event));
        this.editorContentService.getGlassPane().addEventListener('mousemove', (event) => this.onMouseMove(event));
    }

    ngOnDestroy(): void {
        if (this.selectedRefSubscription !== undefined) this.selectedRefSubscription.unsubscribe();
        this.editorStateSubscription.unsubscribe();
        this.removeSelectionChangedListener();
        this.editorContentService.removeContentMessageListener(this);
    }

    ngAfterViewInit(): void {
        this.editorContentService.executeOnlyAfterInit(() => {
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

                this.editorContentService.executeOnlyAfterInit(() => {
                    this.redrawDecorators();
                    if (this.editorSession.getState().showWireframe) {
                        this.applyWireframe();
                    }
                });
            }
        });
    }
    redrawDecorators() {
        if (this.nodes.length > 0) {
            Array.from(this.nodes).forEach(selected => {
                const node = this.editorContentService.getContentElement(selected.svyid);
                if (!node) return;
                const position = node.getBoundingClientRect();
                this.designerUtilsService.adjustElementRect(node, position);
                selected.style = {
                    height: position.height + 'px',
                    width: position.width + 'px',
                    top: position.top + this.topAdjust + 'px',
                    left: position.left + this.leftAdjust + 'px',
                    display: 'block'
                } as CSSStyleDeclaration;
            });
        }
    }

    selectionChanged(selection: Array<string>, redrawDecorators?: boolean): void {
        if (this.contentInit) {
            this.createNodes(selection);
        }
        //fix this
        if (redrawDecorators) {
       //     setTimeout(() => {
                this.redrawDecorators();
       //     }, 400);
        }
    }

    contentMessageReceived(id: string, data: { property: string }) {
        if (id === 'redrawDecorators') {
            this.selectionChanged(this.editorSession.getSelection(), true);
        }
    }

    private createNodes(selection: Array<string>) {
        this.createNodesImpl(selection);
    }

    private createNodesImpl(selection: Array<string>) {
        if (selection.length > 0) {
            this.editorContentService.executeOnlyAfterInit(() => {
                const newNodes = new Array<SelectionNode>();
                const elements = this.editorContentService.getAllContentElements();
                Array.from(elements).forEach(node => {
                    if (selection.indexOf(node.getAttribute('svy-id')) >= 0) {
                        const position = node.getBoundingClientRect();
                        this.designerUtilsService.adjustElementRect(node, position);
                        const style = {
                            height: position.height + 'px',
                            width: position.width + 'px',
                            top: position.top + this.topAdjust + 'px',
                            left: position.left + this.leftAdjust + 'px',
                            display: 'block'
                        } as CSSStyleDeclaration;
                        const layoutName = node.getAttribute('svy-layoutname');
                        newNodes.push({
                            style: style,
                            isResizable: this.urlParser.isAbsoluteFormLayout() && !node.parentElement.closest('.svy-responsivecontainer') ? { t: true, l: true, b: true, r: true } : { t: false, l: false, b: false, r: false },
                            svyid: node.getAttribute('svy-id'),
                            isContainer: layoutName != null && !node.closest('.svy-responsivecontainer'),
                            maxLevelDesign: node.classList.contains('maxLevelDesign'),
                            containerName: layoutName,
                            autowizardProperties:  this.editorSession.getWizardProperties(node.getAttribute('svy-formelement-type'))
                        })
                    }
                });
                this.nodes = newNodes;
            });
        }
    }

    private calculateAdjustToMainRelativeLocation() {
        if (!this.topAdjust) {
            const content = this.editorContentService.getContentArea();
            this.contentRect = content.getBoundingClientRect()
            const computedStyle = window.getComputedStyle(content, null)
            this.topAdjust = parseInt(computedStyle.getPropertyValue('padding-left').replace('px', ''));
            this.leftAdjust = parseInt(computedStyle.getPropertyValue('padding-top').replace('px', ''))
        }
    }

    private onMouseDown(event: MouseEvent) {
        this.fieldLocation = { x: event.pageX, y: event.pageY };
        if (this.editorSession.getState().dragging) return;
        const found = this.designerUtilsService.getNode(event);
        if (found) {
            if (this.editorSession.getSelection().indexOf(found.getAttribute('svy-id')) !== -1) {
                return;  //already selected
            }
            else if (!event.ctrlKey && !event.metaKey && !event.shiftKey) {
                this.editorSession.setSelection([found.getAttribute('svy-id')], this);
            }
        }
        else {
            //lasso select
            this.nodes = [];
            this.editorSession.setSelection([], this);

            this.renderer.setStyle(this.lassoRef.nativeElement, 'left', event.pageX + this.editorContentService.getGlassPane().scrollLeft - this.contentRect.left + 'px');
            this.renderer.setStyle(this.lassoRef.nativeElement, 'top', event.pageY + this.editorContentService.getGlassPane().scrollTop - this.contentRect.top + 'px');
            this.renderer.setStyle(this.lassoRef.nativeElement, 'width', '0px');
            this.renderer.setStyle(this.lassoRef.nativeElement, 'height', '0px');

            this.lassostarted = true;
            this.mousedownpoint = { x: event.pageX, y: event.pageY };
        }
    }

    private onMouseUp(event: MouseEvent) {
        if (this.fieldLocation && this.fieldLocation.x == event.pageX && this.fieldLocation.y == event.pageY) {
            this.editorSession.updateFieldPositioner({ x: event.pageX + this.editorContentService.getGlassPane().scrollLeft - this.contentRect.left - this.leftAdjust, y: event.pageY + this.editorContentService.getGlassPane().scrollTop - this.contentRect.top - this.topAdjust });
        }
        this.fieldLocation = null;
        if (this.editorSession.getState().dragging) return;
        if (event.button == 2 && this.editorSession.getSelection().length > 1) {
            //if we right click on the selected element while multiple selection, just show context menu and do not modify selection
            const node = this.designerUtilsService.getNode(event);
            if (node && this.editorSession.getSelection().indexOf(node.getAttribute('svy-id')) !== -1) {
                return;
            }
        }
        if (this.lassostarted && this.mousedownpoint.x != event.pageX && this.mousedownpoint.y != event.pageY) {
            const elements = this.editorContentService.getAllContentElements();
            const newNodes = new Array<SelectionNode>();
            const newSelection = new Array<string>();
            Array.from(elements).forEach((node) => {
                const position = node.getBoundingClientRect();
                this.designerUtilsService.adjustElementRect(node, position);
                const iframeLeft = this.editorContentService.getLeftPositionIframe();
                const iframeTop = this.editorContentService.getTopPositionIframe();
                if (this.rectangleContainsPoint({ x: event.pageX, y: event.pageY }, { x: this.mousedownpoint.x, y: this.mousedownpoint.y }, { x: position.x + iframeLeft, y: position.y + iframeTop }) ||
                    this.rectangleContainsPoint({ x: event.pageX, y: event.pageY }, { x: this.mousedownpoint.x, y: this.mousedownpoint.y }, { x: position.x + iframeLeft + position.width, y: position.y + iframeTop }) ||
                    this.rectangleContainsPoint({ x: event.pageX, y: event.pageY }, { x: this.mousedownpoint.x, y: this.mousedownpoint.y }, { x: position.x + iframeLeft, y: position.y + iframeTop + position.height }) ||
                    this.rectangleContainsPoint({ x: event.pageX, y: event.pageY }, { x: this.mousedownpoint.x, y: this.mousedownpoint.y }, { x: position.x + iframeLeft + position.width, y: position.y + iframeTop + position.height })) {
                    const layoutName = node.getAttribute('svy-layoutname');
                    const newNode: SelectionNode = {
                        style: {
                            height: position.height + 'px',
                            width: position.width + 'px',
                            top: position.top + this.topAdjust + 'px',
                            left: position.left + this.leftAdjust + 'px',
                            display: 'block'
                        } as CSSStyleDeclaration,
                        svyid: node.getAttribute('svy-id'),
                        isResizable: this.urlParser.isAbsoluteFormLayout() && !node.parentElement.closest('.svy-responsivecontainer') ? { t: true, l: true, b: true, r: true } : { t: false, l: false, b: false, r: false },
                        isContainer: layoutName != null && !node.closest('.svy-responsivecontainer'),
                        maxLevelDesign: node.classList.contains('maxLevelDesign'),
                        containerName: layoutName,
                        autowizardProperties:  this.editorSession.getWizardProperties(node.getAttribute('svy-formelement-type'))
                    };
                    newNodes.push(newNode);
                    newSelection.push(node.getAttribute('svy-id'))
                }
            });
            this.nodes = newNodes;
            this.editorSession.setSelection(newSelection, this);
        }
        else {
            const point = { x: event.pageX, y: event.pageY };
            point.x = point.x - this.editorContentService.getLeftPositionIframe();
            point.y = point.y - this.editorContentService.getTopPositionIframe();
            this.calculateAdjustToMainRelativeLocation();

            const elements = this.editorContentService.getAllContentElements();
            Array.from(elements).reverse().find((node) => {
                const position = node.getBoundingClientRect();
                this.designerUtilsService.adjustElementRect(node, position);
                let addToSelection = false;
                if (node['offsetParent'] !== null && position.x <= point.x && position.x + position.width >= point.x && position.y <= point.y && position.y + position.height >= point.y) {
                    addToSelection = true;
                }
                else if (node['offsetParent'] !== null && parseInt(window.getComputedStyle(node, ':before').height) > 0) {
                    const computedStyle = window.getComputedStyle(node, ':before');
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
                    const layoutName = node.getAttribute('svy-layoutname');
                    const newNode = {
                        style: {
                            height: position.height + 'px',
                            width: position.width + 'px',
                            top: position.top + this.topAdjust + 'px',
                            left: position.left + this.leftAdjust + 'px',
                            display: 'block'
                        } as CSSStyleDeclaration,
                        isResizable: this.urlParser.isAbsoluteFormLayout() && !node.parentElement.closest('.svy-responsivecontainer') ? { t: true, l: true, b: true, r: true } : { t: false, l: false, b: false, r: false },
                        svyid: node.getAttribute('svy-id'),
                        isContainer: layoutName != null && !node.closest('.svy-responsivecontainer'),
                        maxLevelDesign: node.classList.contains('maxLevelDesign'),
                        containerName: layoutName,
                        autowizardProperties:  this.editorSession.getWizardProperties(node.getAttribute('svy-formelement-type'))
                    };
                    if (event.ctrlKey || event.metaKey) {
                        const index = selection.indexOf(id);
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
                        const newNodes = new Array<SelectionNode>();
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
        }
        this.lassostarted = false;
        this.renderer.setStyle(this.lassoRef.nativeElement, 'display', 'none');
        this.applyWireframe();
    }

    private applyWireframe() {
        Array.from(this.selectedRef).forEach((selectedNode) => {
            this.applyWireframeForNode(selectedNode);
        });
    }

    applyWireframeForNode(selectedNode: ElementRef<HTMLElement>) {
        const node = this.editorContentService.getContentElement(selectedNode.nativeElement.getAttribute('id'));
        if (node === undefined) return;
        const position = node.getBoundingClientRect();
        // TODO is && node.getAttribute('svy-layoutname') needed??
        if (node.classList.contains('svy-layoutcontainer') && !node.getAttribute('data-maincontainer') && !node.classList.contains('svy-responsivecontainer') && position.width > 0 && position.height > 0) {
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
                this.renderer.setStyle(this.lassoRef.nativeElement, 'left', event.pageX + this.editorContentService.getGlassPane().scrollLeft - this.contentRect.left + 'px');
            }
            if (event.pageY < this.mousedownpoint.y) {
                this.renderer.setStyle(this.lassoRef.nativeElement, 'top', event.pageY + this.editorContentService.getGlassPane().scrollTop - this.contentRect.top + 'px');
            }
            if (this.lassoRef.nativeElement.style.display === 'none') {
                this.renderer.setStyle(this.lassoRef.nativeElement, 'display', 'block');
            }
            const currentWidth = event.pageX - this.mousedownpoint.x;
            const currentHeight = event.pageY - this.mousedownpoint.y;
            this.renderer.setStyle(this.lassoRef.nativeElement, 'width', Math.abs(currentWidth) + 'px');
            this.renderer.setStyle(this.lassoRef.nativeElement, 'height', Math.abs(currentHeight) + 'px');
        }
    }

    private rectangleContainsPoint(p1: Point, p2: Point, toCheck: Point): boolean {
        if (p1.x > p2.x) {
            const temp = p1.x;
            p1.x = p2.x;
            p2.x = temp;
        }
        if (p1.y > p2.y) {
            const temp = p1.y;
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
        this.editorSession.keyPressed({ 'keyCode': 46 });
    }

    zoomInAction(event: MouseEvent) {
        event.stopPropagation();
        this.editorSession.executeAction('zoomIn');
    }

    copyAction(event: MouseEvent) {
        event.stopPropagation();
        this.editorSession.executeAction('copy');
    }
    
    openWizardAction(event: MouseEvent, property: string) {
        event.stopPropagation();
        this.editorSession.openConfigurator(property);
    }

    insertACopyAction(event: MouseEvent, node: SelectionNode, before: boolean) {
        event.stopPropagation();
        const component = {}
        const htmlNode = this.editorContentService.getContentElement(node.svyid);

        const layoutPackage = htmlNode.getAttribute('svy-layoutname').split('.');
        component['packageName'] = layoutPackage[0];
        component['name'] = layoutPackage[1];
        const droptarget = (htmlNode.parentNode as HTMLElement).getAttribute('svy-id');
        if (droptarget) component['dropTargetUUID'] = droptarget;
        if (before) {
            component['rightSibling'] = node.svyid;
        }
        else
            if (htmlNode.nextElementSibling) {
                component['rightSibling'] = htmlNode.nextElementSibling.getAttribute('svy-id');
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

    constructor(private editorContentService: EditorContentService, private elementRef: ElementRef<HTMLElement>) {

    }

    ngOnInit(): void {
        const htmlNode = this.editorContentService.getContentElement(this.selectionNode.svyid);
        if (parseInt(window.getComputedStyle(htmlNode, ':before').height) > 0) {
            const computedStyle = window.getComputedStyle(htmlNode, ':before');
            const left = parseInt(window.getComputedStyle(htmlNode, null).getPropertyValue('padding-left')) + parseInt(computedStyle.marginLeft);
            const right = htmlNode.getBoundingClientRect().width - left - parseInt(computedStyle.width);
            this.elementRef.nativeElement.style.marginRight = right + 'px';
        }
    }
}

export class SelectionNode {
    svyid: string;
    style: CSSStyleDeclaration;
    isResizable?: ResizeDefinition;
    isContainer: boolean;
    maxLevelDesign: boolean;
    containerName: string;
    autowizardProperties?: string[];
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
