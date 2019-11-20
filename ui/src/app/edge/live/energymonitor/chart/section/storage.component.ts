import { Component, OnDestroy } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from '../../../../../shared/service/defaulttypes';
import { Service, Utils } from '../../../../../shared/shared';
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from './abstractsection.component';
import { UnitvaluePipe } from 'src/app/shared/pipe/unitvalue/unitvalue.pipe';
import { trigger, state, style, animate, transition } from '@angular/animations';


@Component({
    selector: '[storagesection]',
    templateUrl: './storage.component.html',
    animations: [
        trigger('Discharge', [
            state('show', style({
                opacity: 0.4,
                transform: 'translateY(0)'
            })),
            state('hide', style({
                opacity: 0.1,
                transform: 'translateY(-17%)'
            })),
            transition('show => hide', animate('650ms ease-out')),
            transition('hide => show', animate('0ms ease-in'))
        ]),
        trigger('Charge', [
            state('show', style({
                opacity: 0.1,
                transform: 'translateY(0)'
            })),
            state('hide', style({
                opacity: 0.4,
                transform: 'translateY(17%)'
            })),
            transition('show => hide', animate('650ms ease-out')),
            transition('hide => show', animate('0ms ease-out'))
        ])
    ]
})
export class StorageSectionComponent extends AbstractSection implements OnDestroy {

    private socValue: number
    private unitpipe: UnitvaluePipe;
    // animation variable to stop animation on destroy
    private startAnimation = null;
    private showChargeAnimation: boolean = false;
    private showDischargeAnimation: boolean = false;
    public chargeAnimationTrigger: boolean = false;
    public dischargeAnimationTrigger: boolean = false;

    constructor(
        translate: TranslateService,
        service: Service,
        unitpipe: UnitvaluePipe,
    ) {
        super('Edge.Index.Energymonitor.Storage', "down", "#009846", translate, service, "Storage");
        this.unitpipe = unitpipe;
    }

    toggleCharge() {
        this.startAnimation = setInterval(() => {
            this.showChargeAnimation = !this.showChargeAnimation;
        }, this.animationSpeed);
        this.chargeAnimationTrigger = true;
        this.dischargeAnimationTrigger = false;
    }

    toggleDischarge() {
        setInterval(() => {
            this.showDischargeAnimation = !this.showDischargeAnimation;
        }, this.animationSpeed);
        this.chargeAnimationTrigger = false;
        this.dischargeAnimationTrigger = true;
    }

    get stateNameCharge() {
        return this.showChargeAnimation ? 'show' : 'hide'
    }

    get stateNameDischarge() {
        return this.showDischargeAnimation ? 'show' : 'hide'
    }

    protected getStartAngle(): number {
        return 136;
    }

    protected getEndAngle(): number {
        return 224;
    }

    protected getRatioType(): Ratio {
        return 'Negative and Positive [-1,1]';
    }

    public _updateCurrentData(sum: DefaultTypes.Summary): void {
        if (sum.storage.effectiveChargePower != null) {
            let arrowIndicate: number;
            // only reacts to kW values (50 W => 0.1 kW rounded)
            if (sum.storage.effectiveChargePower > 49) {
                if (!this.chargeAnimationTrigger) {
                    this.toggleCharge();
                }
                arrowIndicate = Utils.divideSafely(sum.storage.effectiveChargePower, sum.system.totalPower);
            } else {
                arrowIndicate = 0;
            }

            this.name = this.translate.instant('Edge.Index.Energymonitor.StorageCharge');
            super.updateSectionData(
                sum.storage.effectiveChargePower,
                sum.storage.powerRatio,
                arrowIndicate);
        } else if (sum.storage.effectiveDischargePower != null) {
            let arrowIndicate: number;
            if (sum.storage.effectiveDischargePower > 49) {
                if (!this.dischargeAnimationTrigger) {
                    this.toggleDischarge();
                }
                arrowIndicate = Utils.multiplySafely(
                    Utils.divideSafely(sum.storage.effectiveDischargePower, sum.system.totalPower), -1);
            } else {
                arrowIndicate = 0;
            }
            this.name = this.translate.instant('Edge.Index.Energymonitor.StorageDischarge');
            super.updateSectionData(
                sum.storage.effectiveDischargePower,
                sum.storage.powerRatio,
                arrowIndicate);
        } else {
            this.name = this.translate.instant('Edge.Index.Energymonitor.Storage')
            super.updateSectionData(null, null, null);
        }

        this.socValue = sum.storage.soc;
        if (this.square) {
            this.square.image.image = "assets/img/" + this.getImagePath();
        }
    }

    protected getSquarePosition(square: SvgSquare, innerRadius: number): SvgSquarePosition {
        let x = (square.length / 2) * (-1);
        let y = innerRadius - 5 - square.length;
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        if (this.socValue < 11) {
            return "storage_10_monitor.png"
        } else if (this.socValue < 21) {
            return "storage_20_monitor.png"
        } else if (this.socValue < 31) {
            return "storage_30_monitor.png"
        } else if (this.socValue < 41) {
            return "storage_40_monitor.png"
        } else if (this.socValue < 51) {
            return "storage_50_monitor.png"
        } else if (this.socValue < 61) {
            return "storage_60_monitor.png"
        } else if (this.socValue < 71) {
            return "storage_70_monitor.png"
        } else if (this.socValue < 81) {
            return "storage_80_monitor.png"
        } else if (this.socValue < 91) {
            return "storage_90_monitor.png"
        } else if (this.socValue < 101) {
            return "storage_100_monitor.png"
        } else {
            return "storage_empty_monitor.png"
        }
    }

    protected getValueText(value: number): string {
        if (value == null || Number.isNaN(value)) {
            return "";
        }
        return this.unitpipe.transform(value, 'kW');
    }

    protected initEnergyFlow(radius: number): EnergyFlow {
        return new EnergyFlow(radius, { x1: "50%", y1: "0%", x2: "50%", y2: "100%" });
    }

    // no adjustments needed
    protected setElementHeight() { }

    protected getSvgEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        let v = Math.abs(ratio);
        let r = radius;
        let p = {
            topLeft: { x: v * -1, y: v },
            bottomLeft: { x: v * -1, y: r },
            topRight: { x: v, y: v },
            bottomRight: { x: v, y: r },
            middleBottom: { x: 0, y: r - v },
            middleTop: { x: 0, y: 0 }
        }
        if (ratio > 0) {
            // towards bottom
            p.bottomLeft.y = p.bottomLeft.y - v;
            p.middleBottom.y = p.middleBottom.y + v;
            p.bottomRight.y = p.bottomRight.y - v;
            p.middleTop.y = p.topLeft.y + v;
        }
        return p;
    }

    protected getSvgAnimationEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        let v = Math.abs(ratio);
        let r = radius;
        let animationWidth = r - v;
        let p = {
            topLeft: { x: v * -1, y: v },
            bottomLeft: { x: v * -1, y: r },
            topRight: { x: v, y: v },
            bottomRight: { x: v, y: r },
            middleBottom: { x: 0, y: r - v },
            middleTop: { x: 0, y: 0 }
        }
        if (ratio < 0) {
            // towards top
            p.middleTop.y = p.middleBottom.y + animationWidth * 0.2;
            p.topRight.y = p.bottomRight.y + animationWidth * 0.2;
            p.topLeft.y = p.bottomLeft.y + animationWidth * 0.2;
        } else if (ratio > 0) {
            // towards bottom
            p.bottomLeft.y = p.topLeft.y - animationWidth * 0.2;
            p.middleBottom.y = p.middleTop.y - animationWidth * 0.2 + 2 * v;
            p.bottomRight.y = p.topRight.y - animationWidth * 0.2;
            p.middleTop.y = p.middleBottom.y + animationWidth * 0.2;
        } else {
            p = null;
        }
        return p;
    }

    ngOnDestroy() {
        clearInterval(this.startAnimation);
    }
}