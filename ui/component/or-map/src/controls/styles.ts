import { css } from "lit";

export const countBadgeStyle = css`
    or-vaadin-badge {
        --vaadin-badge-background: var(--shades-contrast-10, #3A463A0D);
        border-radius: calc(var(--lumo-border-radius-m) + 2px);
        font-size: 12px;
        color: var(--lumo-secondary-text-color);
        padding: 0 calc(var(--lumo-space-m) - 4px);
    }
`;
