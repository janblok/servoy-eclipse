import { Component, OnInit, Renderer2, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { EditorSessionService, ISelectionChangedListener } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';
import { Point } from '../mouseselection/mouseselection.component';
import { EditorContentService, IContentMessageListener } from '../services/editorcontent.service';

@Component({
    selector: 'designer-ghostscontainer',
    templateUrl: './ghostscontainer.component.html',
    styleUrls: ['./ghostscontainer.component.css']
})
export class GhostsContainerComponent implements OnInit, ISelectionChangedListener, OnDestroy, IContentMessageListener {

    @ViewChild('element', { static: false }) elementRef: ElementRef<Element>;

    ghostOffset = 20;
    containerLeftOffset: number;
    containerTopOffset: number;
    leftOffsetRelativeToSelectedGhost: number;
    topOffsetRelativeToSelectedGhost: number;

    ghosts: Array<GhostContainer>;
    removeSelectionChangedListener: () => void;
    mousedownpoint: Point;
    draggingGhost: Ghost;
    draggingInGhostContainer: GhostContainer;
    draggingClone: Element;
    draggingGhostComponent: HTMLElement;
    formWidth: number;
    formHeight: number;
    partTopPosition: number;
    
    ghostsVisible = true;
    setFormSizeOnMouseUp = false;
    

    constructor(protected readonly editorSession: EditorSessionService, protected readonly renderer: Renderer2,
        protected urlParser: URLParserService, private editorContentService: EditorContentService) {
        this.editorContentService.addContentMessageListener(this);
        this.removeSelectionChangedListener = this.editorSession.addSelectionChangedListener(this);
    }

    ngOnInit(): void {
        if (this.urlParser.isAbsoluteFormLayout()) {
            // for responsive this is way too early we need the real content size to show the ghosts
            this.renderGhosts()
        }
        this.editorContentService.getContentArea().addEventListener('mouseup', (event: MouseEvent) => this.onMouseUp(event));
        this.editorContentService.getContentArea().addEventListener('mousemove', (event: MouseEvent) => this.onMouseMove(event));
    }

    ngOnDestroy(): void {
        this.removeSelectionChangedListener();
        this.editorContentService.removeContentMessageListener(this);
    }

    contentMessageReceived(id: string, data: { property: string, width? : number, height? : number }) {
        if (id === 'renderGhosts') {
            this.renderGhosts();
        }
        // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
        if (id === 'updateFormSize' && this.urlParser.isAbsoluteFormLayout()) {
            this.formWidth = data.width;
            this.formHeight = data.height;
            this.renderGhosts();
        }
        
        if (id === 'redrawDecorators') {
            if (this.ghosts) {
                for (const ghostContainer of this.ghosts) {
                    for (const ghost of ghostContainer.ghosts) {
                        if (this.editorSession.getSelection().indexOf(ghost.uuid) >= 0) {
                            this.renderGhosts();
                            return;
                        }
                    }
                }
            }
        }
        
        if (id !== 'hideGhostContainer' && !this.ghostsVisible) {
			this.ghostsVisible = true;
			this.hideShowGhosts('visible');
		} else if (id === 'hideGhostContainer' && this.ghostsVisible) {
			this.ghostsVisible = false;
			this.hideShowGhosts('hidden');
		}
    }
    
    hideShowGhosts(visibility: string) {
		const ghostsContainer = document.querySelectorAll(`.${this.elementRef.nativeElement.classList.value}`);
		Array.from(ghostsContainer).slice(1).forEach((item: HTMLElement) => {
			item.style.visibility = visibility; 
			item.querySelectorAll('.ghost').forEach((ghost:HTMLElement) => ghost.style.visibility = visibility);
		});
	}

    renderGhosts() {
        void this.editorSession.getGhostComponents<{ ghostContainers: Array<GhostContainer> }>().then((result: { ghostContainers: Array<GhostContainer> }) => {
            this.renderGhostsInternal(result.ghostContainers);
        });
    }

    private renderGhostsInternal(ghostContainers: Array<GhostContainer>) {
        if (!this.formWidth) {
            this.formWidth = this.urlParser.getFormWidth();
        }
        if (!this.formHeight) {
            this.formHeight = this.urlParser.getFormHeight();
        }
        if (ghostContainers) {
            // set the ghosts
            for (const ghostContainer of ghostContainers) {
                if (!ghostContainer.style) ghostContainer.style = {} as CSSStyleDeclaration;
                if (ghostContainer.containerPositionInComp != undefined) {
                    const odd = (ghostContainer.containerPositionInComp % 2);
                    ghostContainer.style['background-color'] = odd ? 'rgba(150, 150, 150, 0.05)' : 'rgba(0, 100, 80, 0.05)';
                    ghostContainer.style['color'] = odd ? 'rgb(150, 150, 150)' : 'rgb(0, 100, 80)';
                    if (odd) {
                        ghostContainer.style['border-top'] = ghostContainer.style['border-bottom'] = 'dashed 1px';
                    }
                }
                if (!this.urlParser.isAbsoluteFormLayout()) {
                    const node = this.editorContentService.getContentElement(ghostContainer.uuid);
                    if (node !== undefined) {
                        ghostContainer.parentCompBounds = node.getBoundingClientRect();
                    }
                }
                if (ghostContainer.parentCompBounds) {
                    const spaceForEachContainer = 62 /*(basicWebComponent.getSize().height / totalGhostContainersOfComp)*/;
                    let emptySpaceTopBeforGhosts = 0; // see if we have room to add some extra pixels to top location - it shows nicer on large components when space is available
                    if (ghostContainer.parentCompBounds.height > ghostContainer.totalGhostContainersOfComp * spaceForEachContainer + 30) emptySpaceTopBeforGhosts = 30;

                    ghostContainer.style.left = (ghostContainer.parentCompBounds.left + 20) + 'px';
                    ghostContainer.style.top = (ghostContainer.parentCompBounds.top + ghostContainer.containerPositionInComp * spaceForEachContainer + emptySpaceTopBeforGhosts + 20) + 'px';
                    ghostContainer.style.width = ghostContainer.parentCompBounds.width + 'px';
                    ghostContainer.style.height = spaceForEachContainer + 'px';
                }
                else if (this.urlParser.isAbsoluteFormLayout()) {
                    ghostContainer.style.left = '20px';
                    ghostContainer.style.top = '20px';
                    ghostContainer.style.width = this.formWidth + 'px';
                    ghostContainer.style.height = this.formHeight + 'px';
                }

                for (const ghost of ghostContainer.ghosts) {
                    if (ghost.type == GHOST_TYPES.GHOST_TYPE_GROUP) {
                        ghostContainer.style.display = 'none';
                        // groups are deprecated in new designer
                        continue;
                    }
                    let style = {};
                    ghost.hrstyle = { display: 'none' } as CSSStyleDeclaration;
                    if (ghost.type == GHOST_TYPES.GHOST_TYPE_PART) { // parts
                        style = {
                            background: '#d0d0d0',
                            top: ghost.location.y + 'px',
                            right: '-90px',
                            width: '90px',
                            height: '20px',
                            overflow: 'visible'
                        };
                        ghost.hrstyle = {
                            marginTop: '-1px',
                            borderTop: '1px dashed #000',
                            height: '0px',
                            width: (this.formWidth + 90) + 'px',
                            cursor: 'ns-resize',
                            float: 'right'
                        } as CSSStyleDeclaration;
                    } else if (ghost.type == GHOST_TYPES.GHOST_TYPE_FORM) { // the form
                        // why should we need this, it interferes a lot with the events
                        style = {
                            display: 'none'/*
                            left: 0,
                            top: 0,
                            width: ghost.size.width + "px",
                            height: ghost.size.height + "px",
                            padding: "3px"*/
                        };
                    } else {
                        let xOffset = this.ghostOffset;
                        let yOffset = this.ghostOffset;
                        if (ghost.type == GHOST_TYPES.GHOST_TYPE_COMPONENT) {
                            // show outside component at exact location
                            xOffset = 0;
                            yOffset = 0;
                        }
                        // SOME of these are set directly in editor.css for .ghost-dnd-placeholder; if you change things that should affect
                        // the placeholder used when moving droppable config custom objects via drag&drop, please change them in editor.css as well
                        style = {
                            opacity: 0.7,
                            padding: '3px',
                            left: ghost.location.x + xOffset + 'px',
                            top: ghost.location.y + yOffset + 'px',
                            width: ghost.size.width + 'px',
                            height: ghost.size.height + 'px'
                        };
                        if (ghost.type == GHOST_TYPES.GHOST_TYPE_INVISIBLE) {
                            style['background'] = '#d0d0d0';
                        }
                        else if (ghost.type == GHOST_TYPES.GHOST_TYPE_CONFIGURATION) {
                            style['background'] = '#ffbb37';
                        }
                        else if (ghost.type != GHOST_TYPES.GHOST_TYPE_GROUP) {
                            style['background'] = '#e4844a';
                        }
                    }
                    if (this.editorSession.getSelection().indexOf(ghost.uuid) >= 0) {
                        style['background'] = '#07f';
                        style['color'] = '#fff';
                    }
                    ghost.style = style;
                }
            }
        }
        this.ghosts = ghostContainers;
    }

    openContainedForm(ghost: Ghost) {
        if (ghost.type != GHOST_TYPES.GHOST_TYPE_PART) {
            this.editorSession.openContainedForm(ghost.uuid);
        }
    }

    onMouseDown(event: MouseEvent, ghost: Ghost, ghostContainer: GhostContainer) {
        this.editorSession.setSelection([ghost.uuid]);
        if (event.button == 0) {
            this.editorSession.getState().dragging = true;
            this.mousedownpoint = { x: event.pageX, y: event.pageY };
            this.draggingGhost = ghost;
            this.draggingInGhostContainer = ghostContainer;
            if (this.draggingGhost.type == GHOST_TYPES.GHOST_TYPE_CONFIGURATION || this.draggingGhost.type === GHOST_TYPES.GHOST_TYPE_COMPONENT) {
                const parentRect = (event.currentTarget as Element).parentElement.getBoundingClientRect();
                this.containerLeftOffset = parentRect.left;
                this.containerTopOffset = parentRect.top;
                const rect = (event.currentTarget as Element).getBoundingClientRect();
                this.leftOffsetRelativeToSelectedGhost = event.clientX - rect.left; //x position within the element.
                this.topOffsetRelativeToSelectedGhost = event.clientY - rect.top;  //y position within the element.
            }
            if (this.draggingGhost.type == GHOST_TYPES.GHOST_TYPE_CONFIGURATION) {
                this.draggingClone = (event.currentTarget as Element).cloneNode(true) as Element;
                this.renderer.setStyle(this.draggingClone, 'background', '#ffbb37');
            }
        }
    }

    private onMouseUp(event: MouseEvent) {
        if (this.draggingGhost) {
            if (this.mousedownpoint.y != event.pageY || this.mousedownpoint.x != event.pageX) {
                if (this.draggingGhost.type === GHOST_TYPES.GHOST_TYPE_PART) {
                    const changes = {};
                    if (this.setFormSizeOnMouseUp) {
                        const id = document.querySelector('.ghost[svy-ghosttype="form"]').getAttribute('svy-id');
                        this.setFormSizeOnMouseUp = false;
                        changes[id] = { 'y': this.partTopPosition };
                        this.editorSession.sendChanges(changes);
                    } 
                    changes[this.draggingGhost.uuid] = { 'y': this.partTopPosition };
                    this.editorSession.sendChanges(changes);
                }
                if (this.draggingGhost.type == GHOST_TYPES.GHOST_TYPE_CONFIGURATION) {
                    const obj = {};
                    for (const ghost of this.draggingInGhostContainer.ghosts) {
                        obj[ghost.uuid] = { 'x': ghost.location.x, 'y': ghost.location.y };
                    }
                    this.editorSession.sendChanges(obj);
                }
                if (this.draggingGhost.type == GHOST_TYPES.GHOST_TYPE_CONFIGURATION) {
                    this.renderGhostsInternal(this.ghosts);
                }
                if (this.draggingGhost.type == GHOST_TYPES.GHOST_TYPE_COMPONENT) {
                    const obj = {};
                    obj[this.draggingGhost.uuid] = { 'x': event.pageX - this.editorContentService.getLeftPositionIframe() - this.leftOffsetRelativeToSelectedGhost, 'y': event.pageY - this.editorContentService.getTopPositionIframe() - this.topOffsetRelativeToSelectedGhost };
                    this.editorSession.sendChanges(obj);
                    this.renderGhosts();
                }
            }
            // this is just to re-render the decorators
            this.editorSession.updateSelection(this.editorSession.getSelection(), true);
            this.editorSession.getState().dragging = false;
        }
        if (this.draggingClone) {
            this.draggingClone.remove();
            this.draggingClone = null;
        }
        this.draggingGhost = null;
        this.draggingInGhostContainer = null;
        this.draggingGhostComponent = null;
    }

    private onMouseMove(event: MouseEvent) {
        if (this.draggingGhost && (this.mousedownpoint.y != event.pageY || this.mousedownpoint.x != event.pageX)) {
            if (this.draggingGhost.type == GHOST_TYPES.GHOST_TYPE_CONFIGURATION) {
                if (!this.draggingClone.parentNode) {
                    this.editorContentService.getBodyElement().appendChild(this.draggingClone);
                }
                this.renderer.setStyle(this.draggingClone, 'left', (event.pageX - this.leftOffsetRelativeToSelectedGhost) + 'px');
                this.renderer.setStyle(this.draggingClone, 'top', (event.pageY - this.topOffsetRelativeToSelectedGhost) + 'px');
                const initialIndex = this.draggingInGhostContainer.ghosts.indexOf(this.draggingGhost);
                let newIndex = -1;
                const ghostWidth = this.draggingGhost.size.width;
                for (let index = 0; index < this.draggingInGhostContainer.ghosts.length; index++) {
                    const ghostStart = this.draggingInGhostContainer.ghosts[index].location.x + this.ghostOffset;
                    const ghostEnd = ghostStart + ghostWidth;
                    const currentPosition = event.pageX - this.containerLeftOffset;
                    if (currentPosition == ghostStart || currentPosition == ghostEnd) {
                        // on the border, do nothing
                        break;
                    }
                    if (newIndex < 0 && (currentPosition > ghostStart || index == 0) && (currentPosition < ghostEnd || index == this.draggingInGhostContainer.ghosts.length - 1)) {
                        // found its place, is it changed ?
                        if (index == initialIndex) {
                            // everything is fine, do not touch it
                            break;
                        }
                        newIndex = index;
                        this.draggingGhost.location.x = this.draggingInGhostContainer.ghosts[newIndex].location.x;
                        this.draggingGhost.style.left = this.draggingGhost.location.x + this.ghostOffset + 'px';
                    }
                    if (index < initialIndex && newIndex >= 0) {
                        // the newindex was already found, move current ghost to right
                        this.draggingInGhostContainer.ghosts[index].location.x += ghostWidth;
                        this.draggingInGhostContainer.ghosts[index].style.left = this.draggingInGhostContainer.ghosts[index].location.x + this.ghostOffset + 'px';
                    }
                    if (index > initialIndex) {
                        if (newIndex < 0 || newIndex == index) {
                            //move current ghost to left
                            this.draggingInGhostContainer.ghosts[index].location.x -= ghostWidth;
                            this.draggingInGhostContainer.ghosts[index].style.left = this.draggingInGhostContainer.ghosts[index].location.x + this.ghostOffset + 'px';
    
                        }
                    }
                }
                if (newIndex >= 0) {
                    // now all styling is fixed, add ghost in new position so that position is ordered
                    this.draggingInGhostContainer.ghosts.splice(initialIndex, 1);
                    this.draggingInGhostContainer.ghosts.splice(newIndex, 0, this.draggingGhost);
                }
            }
            if (this.draggingGhost.type === GHOST_TYPES.GHOST_TYPE_COMPONENT) {
                if (this.draggingGhostComponent === null) {
                    this.draggingGhostComponent = this.editorContentService.querySelector('[svy-id="' + this.draggingGhost.uuid + '"]');
                }
                this.renderer.setStyle(this.draggingGhostComponent, 'left', (event.pageX - this.containerLeftOffset - this.leftOffsetRelativeToSelectedGhost) + 'px');
                this.renderer.setStyle(this.draggingGhostComponent, 'top', (event.pageY - this.containerTopOffset - this.topOffsetRelativeToSelectedGhost) + 'px');
            }
            if (this.draggingGhost.type === GHOST_TYPES.GHOST_TYPE_PART) {
                const ghostLabel = this.editorContentService.querySelector('[svy-id="' + this.draggingGhost.uuid + '"]');
                const topLimit = ghostLabel.previousSibling ? (ghostLabel.previousElementSibling as HTMLElement).offsetTop + 5 : 5;
                let bottomLimit = ghostLabel.nextSibling ? (ghostLabel.nextElementSibling as HTMLElement).offsetTop - 5 : ghostLabel.clientTop;
                let topLocation = this.draggingGhost.location.y + (event.pageY - this.mousedownpoint.y);
                this.setFormSizeOnMouseUp = false;
                console.log('Mouse move top location: ' + topLocation);
                if (bottomLimit <= 0) {//we are dragging the lowest ghostLabel 
                    topLocation = topLimit > topLocation ? topLimit : topLocation;
                    bottomLimit = topLocation;
                    this.formHeight = topLocation;
                    this.setFormSizeOnMouseUp = true;
                    const container: HTMLElement = this.editorContentService.querySelector('.ghostcontainer');
                    const content: HTMLElement = this.editorContentService.querySelector('.content');
                    this.renderer.setStyle(container, 'height', topLocation + 'px');
                    this.renderer.setStyle(content, 'height', topLocation + 'px');
                }
                if (topLimit <= topLocation && topLocation <= bottomLimit) {
                    this.partTopPosition = topLocation;
                    this.renderer.setStyle(ghostLabel, 'top', topLocation + 'px');
                }
            }
        } 
    }

    selectionChanged(ids: Array<string>, redrawDecorators?: boolean, designerChange?: boolean): void {
        // this is an overkill but sometimes we need the server side data for the ghosts (for example when element was dragged out of form bounds and is shown as ghost)
        // not sure how to detect when we really need to redraw
        if (designerChange) this.renderGhosts();
        //this.renderGhostsInternal(this.ghosts);
    }
}

class GhostContainer {
    propertyName: string;
    uuid: string;
    class: string;
    style: CSSStyleDeclaration;
    parentCompBounds: { left?: number, top?: number; width?: number; height?: number };
    containerPositionInComp: number;
    totalGhostContainersOfComp: number;
    ghosts: Array<Ghost>;
}

class Ghost {
    uuid: string;
    text: string;
    type: string;
    class: string;
    propertyType: string;
    style: { left?: string, top?: string; right?: string; width?: string; height?: string };
    hrstyle: CSSStyleDeclaration;
    location: { x: number, y: number };
    size: { width: number, height: number };
}

export enum GHOST_TYPES {
    GHOST_TYPE_CONFIGURATION = 'config',
    GHOST_TYPE_COMPONENT = 'comp',
    GHOST_TYPE_PART = 'part',
    GHOST_TYPE_FORM = 'form',
    GHOST_TYPE_INVISIBLE = 'invisible',
    GHOST_TYPE_GROUP = 'group'
}