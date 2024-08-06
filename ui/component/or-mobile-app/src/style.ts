import {css, CSSResult} from "lit";

export function getAnimationStyles(): CSSResult {
    return css`
        
        :host {
            --animate-offset: 0ms;
        }

        /* ------------------------------------------- */
        /* Material Design animation based on Shared X axis (https://m2.material.io/design/motion/the-motion-system.html#shared-axis) */
        /* ------------------------------------------- */

        .animate-swipeleft-enter {
            animation: container-swipeleft-enter 300ms cubic-bezier(0.0, 0.0, 0.2, 1) var(--animate-offset);
            animation-fill-mode: forwards;
        }

        .animate-swipeleft-exit {
            animation: container-swipeleft-exit 300ms cubic-bezier(0.0, 0.0, 0.2, 1) var(--animate-offset);
            animation-fill-mode: forwards;
        }

        /* ------------------------------------------- */
        /* Material Design animation based on Shared X axis (https://m2.material.io/design/motion/the-motion-system.html#shared-axis) */
        /* ------------------------------------------- */

        .animate-swiperight-enter {
            animation: container-swiperight-enter 300ms cubic-bezier(0.0, 0.0, 0.2, 1) var(--animate-offset);
            animation-fill-mode: forwards;
        }

        .animate-swiperight-exit {
            animation: container-swiperight-exit 300ms cubic-bezier(0.0, 0.0, 0.2, 1) var(--animate-offset);
            animation-fill-mode: forwards;
        }

        /* ------------------------------------------- */
        /* Material design animation based on https://m2.material.io/design/motion/the-motion-system.html#fade-through */
        /* ------------------------------------------- */

        .animate-fade-enter {
            animation: container-fade-enter 210ms ease-out var(--animate-offset);
            animation-fill-mode: forwards;
        }

        .animate-fade-exit {
            animation: container-fade-exit 90ms ease-in var(--animate-offset);
            animation-fill-mode: forwards;
        }


        /* ------------------------------------------ */


        /* Material Design animation based on Shared X axis (https://m2.material.io/design/motion/the-motion-system.html#shared-axis) */
        @keyframes container-swipeleft-enter {
            0% {
                margin-left: 30px;
                margin-right: -30px;
                opacity: 0;
            }
            33% {
                opacity: 0;
            }
            100% {
                margin-left: 0;
                margin-right: 0;
                opacity: 1;
            }
        }

        /* Material Design animation based on Shared X axis (https://m2.material.io/design/motion/the-motion-system.html#shared-axis) */
        @keyframes container-swipeleft-exit {
            0% {
                margin-left: 0;
                margin-right: 0;
                opacity: 1;
            }
            33% {
                opacity: 0;
            }
            100% {
                margin-left: -30px;
                margin-right: 30px;
                opacity: 0;
            }
        }

        /* Material Design animation based on Shared X axis (https://m2.material.io/design/motion/the-motion-system.html#shared-axis) */
        @keyframes container-swiperight-enter {
            0% {
                margin-left: -30px;
                margin-right: 30px;
                opacity: 0;
            }
            33% {
                opacity: 0;
            }
            100% {
                margin-left: 0;
                margin-right: 0;
                opacity: 1;
            }
        }

        /* Material Design animation based on Shared X axis (https://m2.material.io/design/motion/the-motion-system.html#shared-axis) */
        @keyframes container-swiperight-exit {
            0% {
                margin-left: 0;
                margin-right: 0;
                opacity: 1;
            }
            33% {
                opacity: 0;
            }
            100% {
                margin-left: 30px;
                margin-right: -30px;
                opacity: 0;
            }
        }

        /* Material design animation based on https://m2.material.io/design/motion/the-motion-system.html#fade-through */
        @keyframes container-fade-enter {
            0% {
                transform: scale(0.92);
                opacity: 0;
            }
            100% {
                transform: scale(1);
                opacity: 1;
            }
        }

        /* Material design animation based on https://m2.material.io/design/motion/the-motion-system.html#fade-through */
        @keyframes container-fade-exit {
            0% {
                opacity: 1;
            }
            100% {
                opacity: 0;
            }
        }

    `;
}