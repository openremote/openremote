import {AppStateKeyed, OrApp, Page, PageProvider} from "@openremote/or-app";
import {customElement, state} from "lit/decorators.js";
import {MobilePage, MobilePageAnimation} from "./types";
import {getAnimationStyles} from "./style";
import {loadingPageProvider} from "./loading-page";

export * from "./loading-page";
export * from "./style";
export * from "./types";

/**
 * # Mobile App
 * ### `<or-mobile-app>` - `OrMobileApp`
 *
 * Extended class of {@link OrApp} that includes features useful for building smooth mobile apps. <br />
 * Think of transitions or loaders between pages, additional caching functions, and additional states to obtain data from.
 */
@customElement("or-mobile-app")
export class OrMobileApp<S extends AppStateKeyed> extends OrApp<S> {

    protected loadingPageProvider?: PageProvider<any> = loadingPageProvider(this._store);

    static get styles(): any[] {
        return [super.styles, getAnimationStyles()];
    }

    protected async _loadPage(pageProvider: PageProvider<any>, beforeLoad?: (page: Page<any>) => (Promise<void> | void), animate = true): Promise<Page<any>> {
        const currentPage = Array.from(this._mainElem.children).find(child => child.id !== this.OFFLINE_PAGE_ID) as MobilePage<any> | undefined;
        /*const currentPage = this._mainElem.firstElementChild as MobilePage<any> | undefined;*/
        console.info(`Navigating from ${currentPage?.name} to ${pageProvider.name}, with animation set to ${animate}`);

        // Exit old page
        if (currentPage) {
            await this._unloadPage(currentPage, animate);
        }

        // Loading animation
        let loadingPage: MobilePage<any> | undefined;
        if (animate) {
            loadingPage = this.loadingPageProvider?.pageCreator() as MobilePage<any> | undefined;
            if (loadingPage) {
                await this._applyLoadingPage(loadingPage, false);
            } else {
                console.warn("Could not apply loading page, as it did not exist.")
            }
        }

        // Prepare page load
        const newPage = pageProvider.pageCreator() as MobilePage<any> | undefined;
        await beforeLoad?.(newPage);
        newPage.style.visibility = 'hidden';
        newPage.style.position = 'absolute';

        // Await new page load
        this._mainElem.appendChild(newPage);
        await newPage.getUpdateComplete();
        console.log("New page update complete!");

        // Unload animation
        if (animate) {
            if (loadingPage) {
                await this._unloadLoadingPage(loadingPage, false);
            } else {
                console.warn("Could not unload loading page, as it did not exist.");
            }
        }

        // Make new page visible
        newPage.style.visibility = 'unset';
        newPage.style.position = 'unset';
        if (animate) {
            await this._doEnterAnimation(newPage);
        }

        console.log("Done with transition!");
        return newPage;
    }

    /**
     * Unloads the page from the `<amin>` element.
     * By default, it unloads the existing / current {@link MobilePage}, and animates a fade out.
     *
     * @param page The {@link MobilePage} to unload
     * @param animate Whether to animate a fade out
     * @protected
     */
    protected async _unloadPage(page?: MobilePage<any>, animate = true): Promise<void> {
        console.log("_unloadPage()", page);
        if (page && animate) {
            await this._doExitAnimation(page);
        }
        await super._unloadPage(page);
    }

    /**
     *
     */
    protected async _applyLoadingPage(page: MobilePage<any>, animate = true): Promise<void> {
        page.id = "loading-page";
        this._mainElem.appendChild(page);
        if (animate) {
            await this._doEnterAnimation(page);
        }
    }

    protected async _unloadLoadingPage(page: MobilePage<any> | undefined = this._mainElem.querySelector('#loading-page'), animate = true): Promise<void> {
        if (!page) {
            return;
        }
        if (animate) {
            await this._doExitAnimation(page);
        }
        this._mainElem.removeChild(page);
    }

    protected async _doEnterAnimation(page: MobilePage<any>, animation?: MobilePageAnimation): Promise<void> {
        animation = animation || page.enterAnimation;
        await page.getUpdateComplete();
        switch (animation) {
            case MobilePageAnimation.SWIPE_LEFT: {
                await doAnimation(page, 'animate-swipeleft-enter', 300);
                break;
            }
            case MobilePageAnimation.SWIPE_RIGHT: {
                await doAnimation(page, 'animate-swiperight-enter', 300);
                break;
            }
            default: {
                await doAnimation(page, 'animate-fade-enter', 200);
                break;
            }
        }
    }

    protected async _doExitAnimation(page: MobilePage<any>, animation?: MobilePageAnimation): Promise<void> {
        animation = animation || page.exitAnimation;
        await page.updateComplete;
        switch (animation) {
            case MobilePageAnimation.SWIPE_LEFT: {
                await doAnimation(page, 'animate-swipeleft-exit', 300);
                break;
            }
            case MobilePageAnimation.SWIPE_RIGHT: {
                await doAnimation(page, 'animate-swiperight-exit', 300);
                break;
            }
            default: {
                await doAnimation(page, 'animate-fade-exit', 90);
                break;
            }
        }
    }
}

export async function doAnimation(elem: Element, cssClass: string, timeout: number): Promise<void> {
    elem.classList.add(cssClass);
    await new Promise(resolve => setTimeout(resolve, timeout));
    elem.classList.remove(cssClass);
}