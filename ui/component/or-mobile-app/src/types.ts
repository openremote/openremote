import {AppStateKeyed, Page} from "@openremote/or-app";

export enum MobilePageAnimation {
    SWIPE_RIGHT, SWIPE_LEFT, FADE
}

export abstract class MobilePage<S extends AppStateKeyed> extends Page<S> {

    abstract get name(): string;

    public async getUpdateComplete(): Promise<boolean> {
        return super.getUpdateComplete();
    }

    public get enterAnimation(): MobilePageAnimation {
        return MobilePageAnimation.FADE;
    }

    public get exitAnimation(): MobilePageAnimation {
        return MobilePageAnimation.FADE;
    }
}
