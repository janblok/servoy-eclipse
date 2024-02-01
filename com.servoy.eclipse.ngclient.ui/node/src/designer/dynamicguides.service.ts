import { Injectable, Inject } from '@angular/core';
import { WindowRefService } from '@servoy/public';
import { IDesignFormComponent } from './servoydesigner.component';
import { DesignFormComponent } from './designform_component.component';
import { ComponentCache, StructureCache, FormComponentCache, FormComponentProperties, FormCache, CSSPosition, Position } from '../ngclient/types';
import { DOCUMENT } from '@angular/common';


@Injectable()
export class DynamicGuidesService {
    formComponent: IDesignFormComponent;
    
    private leftPos: Map<string, number> = new Map();
    private rightPos: Map<string, number> = new Map();
    private topPos: Map<string, number> = new Map();
    private bottomPos : Map<string, number> = new Map();
    private middleV: Map<string, number> = new Map();
    private middleH : Map<string, number> = new Map();
    private rectangles : DOMRect[];
    private element: Element;
    
    private snapThreshold: number = 0;
    private equalDistanceThreshold: number = 0;

    constructor( @Inject(DOCUMENT) private document: Document, private windowRefService: WindowRefService) {
		this.windowRefService.nativeWindow.addEventListener('message', (event) => {
			if (event.data.id === 'snapThresholds') { 
                this.snapThreshold = parseInt(event.data.value.alignment, 0);
                this.equalDistanceThreshold = parseInt(event.data.value.distance, 0);
            }
            if (event.data.id === 'getSnapTarget') {
            
                if (this.leftPos.size == 0) {
					this.rectangles = [];
					this.element = this.document.elementsFromPoint(event.data.p1.x, event.data.p1.y).find(e => e.getAttribute('svy-id'));
					const uuid = this.element?.getAttribute('svy-id');
                    for (let comp of this.formComponent.getDesignFormComponent().formCache.componentCache.values()) {
                        if (comp.name == '') continue;
                        const id = comp.name;
                        const bounds = this.document.querySelector("[svy-id='"+id+"']").getBoundingClientRect();
                        this.leftPos.set(id, bounds.left);
                        this.rightPos.set(id, bounds.right);
                        this.topPos.set(id, bounds.top);
                        this.bottomPos.set(id, bounds.bottom);
                        this.middleV.set(id, (bounds.top + bounds.bottom)/2);
                        this.middleH.set(id, (bounds.left + bounds.right)/2);
                        if (id !== uuid) this.rectangles.push(bounds);
                    }
                    const sortfn = (a, b) => a[1] - b[1];
                    this.leftPos = new Map([...this.leftPos].sort(sortfn));
                    this.rightPos = new Map([...this.rightPos].sort(sortfn));
                    this.topPos = new Map([...this.topPos].sort(sortfn));
                    this.bottomPos = new Map([...this.bottomPos].sort(sortfn));
                    this.middleH = new Map([...this.middleH].sort(sortfn));
                    this.middleV = new Map([...this.middleV].sort(sortfn));
                }
            	
                let props = this.getSnapProperties(event.data.p1, event.data.resizing);              
                this.windowRefService.nativeWindow.parent.postMessage({ id: 'snap', properties: props }, '*');
            }
            if (event.data.id === 'clearSnapCache') {
                this.leftPos = new Map();
                this.rightPos = new Map();
                this.topPos = new Map();
                this.bottomPos = new Map();
                this.middleV = new Map();
                this.middleH = new Map();
                this.rectangles = [];
            }
		});
    }

    setDesignFormComponent(designFormComponent: IDesignFormComponent) {
        this.formComponent = designFormComponent;
    }
    
    private isSnapInterval(uuid, coordinate, posMap) {
        for (let [key, value] of posMap) {
            if (key === uuid) continue;
            if ((coordinate > value - this.snapThreshold) && (coordinate < value + this.snapThreshold)) {
                //return the first component id that matches the coordinate
                return {uuid: key};
            }
        }
        return null;        
    }
    
    private getDraggedElementRect(point: {x: number, y: number}): DOMRect {
         let rect = this.element?.getBoundingClientRect();
         const fc = this.formComponent.getDesignFormComponent();
         if (!this.element?.getAttribute('svy-id') && fc.draggedElementItem) {
			 const item = fc.draggedElementItem as ComponentCache;
			 rect = new DOMRect(point.x, point.y, item.model.size.width, item.model.size.height);
		}
        return rect;
    } 
    
	private getSnapProperties(point: { x: number, y: number }, resizing: string) {
		let elem = this.document.elementsFromPoint(point.x, point.y).find(e => e.getAttribute('svy-id'));
		if (!this.formComponent.getDesignFormComponent().draggedElementItem && !resizing) {
			//we always need to update the element when dragging, otherwise it doesn't get out of the snap mode
			this.element = elem;
		}
		if (resizing && elem) {
			//in resize mode, we need the current element to determine the edge(resize knob) we are dragging
			//so we use the mouse position for the correct location
			//(the element rect might not always be up to date, especially when dragging faster) 
			this.element = elem;
		}

		const uuid = this.element?.getAttribute('svy-id');
		if (!uuid && !this.formComponent.getDesignFormComponent().draggedElementItem) return { top: point.y, left: point.x, guides: [] };

		const rect = this.getDraggedElementRect(point);
		let properties = { initPoint: point, top: point.y, left: point.x, cssPosition: {}, guides: [] };
		
		const horizontalSnap = this.handleHorizontalSnap(resizing, point, uuid, rect, properties);
		const verticalSnap = this.handleVerticalSnap(resizing, point, uuid, rect, properties);
		if (!resizing) { //TODO impl, ignore eq dist when resizing for now 
			//equal distance guides
			const verticalDist = this.addEqualDistanceVerticalGuides(rect, properties);
			if (verticalDist && verticalSnap) {
				properties.guides.splice(properties.guides.indexOf(verticalSnap), 1);
			}
			const horizontalDist = this.addEqualDistanceHorizontalGuides(rect, properties);
			if (horizontalDist && horizontalSnap) {
				properties.guides.splice(properties.guides.indexOf(horizontalSnap), 1);
			}
		}

		return properties.guides.length == 0 ? null : properties;
	}

	private handleHorizontalSnap(resizing: string, point: { x: number, y: number }, uuid: string, rect: DOMRect, properties: any) : Guide {
		if (!resizing || resizing.indexOf('e') >= 0 || resizing.indexOf('w') >= 0) {
			let closerToTheLeft = this.pointCloserToTopOrLeftSide(point, rect, 'x');
			let snapX, guideX;
			if (!resizing || closerToTheLeft) {
				snapX = this.isSnapInterval(uuid, resizing ? point.x : rect.left, this.leftPos);
				if (snapX?.uuid) {
					properties.left = this.leftPos.get(snapX.uuid);
				}
				else {
					snapX = this.isSnapInterval(uuid, resizing ? point.x : rect.left, this.rightPos);
					if (snapX?.uuid) {
						properties.left = this.rightPos.get(snapX.uuid);
						snapX.prop = 'right';
					}
				}

				if (snapX) {
					properties.cssPosition['left'] = snapX;
					if (!properties.cssPosition['left']) properties.cssPosition['left'] = properties.left;
					guideX = properties.left;
					if (resizing) {
						properties['width'] = rect.width + rect.left - properties.left;
					}
				}
			}
			//if not found, check the right edge as well
			if(!snapX && (!resizing || !closerToTheLeft)) {
				snapX = this.isSnapInterval(uuid, resizing ? point.x : rect.right, this.rightPos);
				guideX = this.rightPos.get(snapX?.uuid);
				properties.left = snapX ? this.rightPos.get(snapX.uuid) : properties.left;
				if (!snapX) {
					snapX = this.isSnapInterval(uuid, resizing ? point.x : rect.right, this.leftPos);
					if (snapX?.uuid) {
						properties.left = this.leftPos.get(snapX.uuid);
						guideX = this.leftPos.get(snapX.uuid);
						snapX.prop = 'left';
					}
				}
				if (snapX) {
					properties.cssPosition['right'] = snapX;
					if (resizing) {
						properties.left = rect.left;
						properties['width'] = guideX - properties.left;
					}
					else {
						properties.left -= rect.width;
					}
					if (!properties.cssPosition['right']) properties.cssPosition['right'] = properties.left;
				}
			}

			if (!snapX && !resizing) { //TODO impl, ignore middle for now for resizing
				snapX = this.isSnapInterval(uuid, (rect.left + rect.right) / 2, this.middleH);
				if (snapX) {
					properties.cssPosition['middleH'] = snapX;
					properties.left = this.middleH.get(snapX.uuid) - rect.width / 2;
					guideX = this.middleH.get(snapX.uuid);
				}
			}

			if (snapX) {
				let guide : Guide;
				if (this.topPos.get(snapX.uuid) < rect.top) {
					guide = new Guide(guideX, this.topPos.get(snapX.uuid), 1, rect.bottom - this.topPos.get(snapX.uuid), 'snap');
				}
				else {
					guide = new Guide(guideX, rect.top, 1, this.topPos.get(snapX.uuid) - rect.top, 'snap');
				}
				properties.guides.push(guide);
				return guide;
			}
		}
		return null;
	}

	private handleVerticalSnap(resizing: string, point: { x: number, y: number }, uuid: string, rect: DOMRect, properties: any) : Guide {
		if (!resizing || resizing.indexOf('s') >= 0 || resizing.indexOf('n') >= 0) {
			let closerToTheTop = this.pointCloserToTopOrLeftSide(point, rect, 'y');
			let snapY, guideY;
			if (!resizing || closerToTheTop) {
				snapY = this.isSnapInterval(uuid, resizing ? point.y : rect.top, this.topPos);
				if (snapY?.uuid) {
					properties.top = this.topPos.get(snapY.uuid);
				}
				else {
					snapY = this.isSnapInterval(uuid, resizing ? point.y : rect.top, this.bottomPos);
					if (snapY?.uuid) {
						properties.top = this.bottomPos.get(snapY.uuid);
						snapY.prop = 'bottom';
					}
				}

				if (snapY) {
					properties.cssPosition['top'] = snapY;
					guideY = properties.top;
					if (resizing) {
						properties['height'] = rect.height + rect.top - properties.top;
					}
				}
			}
			if (!snapY && (!resizing || !closerToTheTop)) {
				snapY = this.isSnapInterval(uuid, resizing ? point.y : rect.bottom, this.bottomPos);
				if (snapY?.uuid) {
					guideY = this.bottomPos.get(snapY.uuid);
					properties.top = snapY ? this.bottomPos.get(snapY.uuid) : properties.top;
				}
				if (!snapY) {
					snapY = this.isSnapInterval(uuid, resizing ? point.y : rect.bottom, this.topPos);
					if (snapY?.uuid) {
						properties.top = this.topPos.get(snapY.uuid);
						guideY = this.topPos.get(snapY.uuid);
						snapY.prop = 'top';
					}
				}
				if (snapY) {
					properties.cssPosition['bottom'] = snapY;
					if (resizing) {
						properties.top = rect.top;
						properties['height'] = guideY - properties.top;
					}
					else {
						properties.top -= rect.height;
					}
				}
			}
			if (!snapY && !resizing) { //TODO impl, ignore middle for now
				snapY = this.isSnapInterval(uuid, (rect.top + rect.bottom) / 2, this.middleV);
				if (snapY?.uuid) {
					properties.cssPosition['middleV'] = snapY;
					properties.top = this.middleV.get(snapY.uuid) - rect.height / 2;
					guideY = this.middleV.get(snapY.uuid);
				}
			}

			if (snapY) {
				let guide;
				if (this.leftPos.get(snapY.uuid) < rect.left) {
					guide = new Guide(this.leftPos.get(snapY.uuid), guideY, rect.right - this.leftPos.get(snapY.uuid), 1, 'snap');
				}
				else {
					guide = new Guide(rect.left, guideY, this.leftPos.get(snapY.uuid) - rect.left, 1, 'snap');
				}
				properties.guides.push(guide);
				return guide;
			}
		}
		return null;
	}
    
	private pointCloserToTopOrLeftSide(point: {x: number, y: number}, rectangle: DOMRect, axis: 'x' | 'y'): boolean {
		const calculateDistance = (a: number, b: number) => Math.abs(a - b);

		const distanceToStart = axis === 'y' ? calculateDistance(point.y, rectangle.y) : calculateDistance(point.x, rectangle.x);
		const distanceToEnd = axis === 'y' ? calculateDistance(point.y, rectangle.y + rectangle.height) : calculateDistance(point.x, rectangle.x + rectangle.width);

		return distanceToStart < distanceToEnd;
	}
    
    private addEqualDistanceVerticalGuides(rect: DOMRect, properties: any ): Guide[] {
		const overlappingX = this.getOverlappingRectangles(rect, 'x');
        for (let pair of overlappingX){
			const e1 = pair[0];
            const e2 = pair[1];   
            if (e2.top > e1.bottom && rect.top > e2.bottom) {
				const dist = e2.top - e1.bottom;
				if (Math.abs(dist - rect.top + e2.bottom) < this.equalDistanceThreshold) {
					properties.top = e2.bottom + dist;
	    			const r = new DOMRect(properties.left ? properties.left : rect.x, properties.top, rect.width, rect.height);
    				return this.addVerticalGuides(e1, e2, r, dist, properties);
				}
			}
			if (e2.top > e1.bottom && e1.top > rect.bottom) {
				const dist = e2.top - e1.bottom;
				if (Math.abs(dist - e1.top + rect.bottom) < this.equalDistanceThreshold) {
					properties.top = e1.top - dist - rect.height;
	    			const r = new DOMRect(properties.left ? properties.left : rect.x, properties.top, rect.width, rect.height);
    				return this.addVerticalGuides(r, e1, e2, dist, properties);
				}
			}
			if (e2.top > rect.bottom && rect.top > e1.bottom) {
				const dist = (e2.top - e1.bottom) / 2;
				if (Math.abs(e1.bottom + dist - rect.top - rect.height / 2) < this.equalDistanceThreshold) {
					properties.top = e1.bottom + dist - rect.height/2;
	    			const r = new DOMRect(properties.left ? properties.left : rect.x, properties.top, rect.width, rect.height);
    				return this.addVerticalGuides(e1, r, e2, dist - rect.height/2, properties);
				}
			}
		}
		return null;
	}
	
	private addEqualDistanceHorizontalGuides(rect: DOMRect, properties: any ): Guide[] {
		const overlappingY = this.getOverlappingRectangles(rect, 'y');
        for (let pair of overlappingY){
			const e1 = pair[0];
            const e2 = pair[1];
            if (e2.left > e1.right && rect.left > e2.right) {
				const dist = e2.left - e1.right;
				if (Math.abs(dist - rect.left + e2.right) < this.equalDistanceThreshold) {                  
                	properties.left = e2.right + dist;
                 	const r = new DOMRect(properties.left, properties.top ? properties.top : rect.y, rect.width, rect.height);
    				return this.addHorizontalGuides(e1, e2, r, dist, properties);
                }
			}
			if (e1.left > rect.right && e2.left > e1.right) {
				const dist = e2.left - e1.right;
				if (Math.abs(dist - rect.right + e1.left) < this.equalDistanceThreshold) {
               		properties.left = e1.left - dist - rect.width;
                   	const r = new DOMRect(properties.left, properties.top ? properties.top : rect.y, rect.width, rect.height);
                   	return this.addHorizontalGuides(r, e1, e2, dist, properties);
               	}
			}
			if (e2.left > rect.right && rect.left > e1.right)  {
				const dist = (e2.left - e1.right) / 2;   
               	if (Math.abs(e1.right + dist - rect.left - rect.width / 2) < this.equalDistanceThreshold) {
					properties.left = e1.right + dist - rect.width/2;
	    			const r = new DOMRect(properties.left, properties.top ? properties.top : rect.y, rect.width, rect.height);
    				return this.addHorizontalGuides(e1, r, e2, dist - rect.width/2, properties);
				}
			}
		}
		return null;
	}
    
    private getOverlappingRectangles(rect: DOMRect, axis: 'x' | 'y'): DOMRect[][] {
		let overlaps = this.rectangles.filter(r => this.isOverlap(rect, r, axis));
		const pairs: DOMRect[][] = [];
    	for (let i = 0; i < overlaps.length - 1; i++) {
        	for (let j = i + 1; j < overlaps.length; j++) {
            	if (overlaps[i] !== rect && overlaps[j] !== rect) {
                	pairs.push([overlaps[i], overlaps[j]]);
            	}
        	}
    	}
    	return pairs;
	}
    
    private isOverlap(rect: DOMRect, eRect: DOMRect, axis: 'x' | 'y'): boolean {
		if (axis === 'x') {
        	return (rect.left >= eRect.left && rect.left <= eRect.right) ||
               	(rect.right >= eRect.left && rect.right <= eRect.right);
    	} else if (axis === 'y') {
        	return (rect.top >= eRect.top && rect.top <= eRect.bottom) ||
               (rect.bottom >= eRect.top && rect.bottom <= eRect.bottom);
    	}
    	return false;
	}
	
	private getDOMRect(uuid: string) : DOMRect {
		return new DOMRect(this.leftPos.get(uuid), this.topPos.get(uuid), 
                	this.rightPos.get(uuid) - this.leftPos.get(uuid),
                	this.bottomPos.get(uuid) - this.topPos.get(uuid));
	}
	
	private addVerticalGuides(e1: DOMRect, e2: DOMRect, r: DOMRect, dist: number, properties: any): Guide[] {
	    const right = Math.max(r.right, e1.right, e2.right);
	    let guides = [];
	    guides.push(new Guide(e1.right, e1.bottom, this.getGuideLength(right, e1.right), 1, 'dist'));
	    guides.push(new Guide(right + 10, e1.bottom, 1, dist, 'dist'));
	    const len = this.getGuideLength(right, e2.right);
	    guides.push(new Guide(e2.right, e2.top, len, 1, 'dist'));
	    guides.push(new Guide(e2.right, e2.bottom, len, 1, 'dist'));
	    guides.push(new Guide(right + 10, e2.bottom, 1, dist, 'dist'));
	    guides.push(new Guide(r.right, r.top, this.getGuideLength(right, r.right), 1, 'dist'));
	    properties.guides.push(...guides);
	    return guides;
	}
	
	private addHorizontalGuides(e1: DOMRect, e2: DOMRect, r: DOMRect, dist: number, properties: any): Guide[] {
	    let bottom = Math.max(r.bottom, e1.bottom, e2.bottom);
		let guides = [];
	    guides.push(new Guide(e1.right, e1.bottom, 1, this.getGuideLength(bottom, e1.bottom), 'dist'));
	    guides.push(new Guide(e1.right, bottom + 10, dist, 1, 'dist'));
	    const len = this.getGuideLength(bottom, e2.bottom);
	    guides.push(new Guide(e2.left, e2.bottom, 1, len, 'dist'));
	    guides.push(new Guide(e2.right, e2.bottom, 1, len, 'dist'));
	    guides.push(new Guide(e2.right, bottom + 10, dist, 1, 'dist'));
	    guides.push(new Guide(r.left, r.bottom, 1, this.getGuideLength(bottom, r.bottom), 'dist'));
	    properties.guides.push(...guides);
	    return guides;
	}
	
	private getGuideLength(max: number, x: number): number {
		return max - x + 15;
	}
}

class Guide {
	x: number;
	y: number;
	width: number;
	height: number;
	styleClass: string;
	constructor(x: number, y: number,
		width: number, height: number, styleClass: string) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.styleClass = styleClass;
	}
}