import { css } from "lit";

export const EditorWorkspaceStyle = css`
:host{
    background: whitesmoke;
    position: relative;
    display: block;
    overflow: hidden;
    box-shadow: rgba(0, 0, 0, 0.2) 0 0 4px inset;
    height: 100%;
}

.view-options{
    position: absolute;
    left: 0;
    top: 0;
    display: flex;
    flex-direction: row;
}

.button{
    padding: 10px;
    margin: 10px;
    cursor:pointer;
    background: rgba(0,0,0,0.02);
}

or-mwc-input[type=button]
{
    margin: 10px;
    color: inherit;
}

.button:hover{
    background: rgba(0,0,0,0.04);
}

.button:active{
    background: rgba(0,0,0,0.06);
}

svg, connection-container {
    pointer-events: none;
    position: absolute;
    display: block;
    right: 0;
    top: 0;
    left: 0;
    bottom: 0;
    width: 100%;
    height: 100%;
    stroke-width: 4px;
    stroke: rgb(80,80,80);
}
`;
