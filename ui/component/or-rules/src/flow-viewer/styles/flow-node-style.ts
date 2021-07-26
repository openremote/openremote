import { css } from "lit";

export const FlowNodeStyle = css`
:host{
    white-space: nowrap;
    min-width: 80px;
    min-height: 80px;
    background: rgba(200,200,200, 0.85);

    display: grid;
    grid-template-columns: auto auto auto;
    grid-template-rows: auto 1fr;
    grid-template-areas: 
        "title title title"
        "input internal output";

    position: absolute;
    border-radius: var(--roundness);
    transform-origin: 0 0;
    z-index: 0;
}
:host([minimal]){
    min-width: 60px;
    min-height: 60px;
    grid-template-columns: var(--socket-display-size) 1fr var(--socket-display-size);
    grid-template-rows: auto;
    grid-template-areas: 
        "input title output";
}
.internal-container{
    grid-area: internal;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 8px 0 8px 0;
}
.socket-side{
    display: flex;
    flex-direction: column;
    justify-content: center;
    justify-content: space-evenly;
}
.inputs{
    grid-area: input;
    align-items: flex-start;
}
.inputs flow-node-socket{
    transform: translateX(calc(var(--socket-size) / -2));
}
.outputs{
    grid-area: output;
    align-items: flex-end;
}
.outputs flow-node-socket{
    transform: translateX(calc(var(--socket-size) / 2));
}
.title{
    grid-area: title;
    padding: 3px 6px 3px 6px;
    background: rgb(180,180,180);
    border-radius: inherit;
    border-bottom-left-radius: 0;
    border-bottom-right-radius: 0;
    color: white;
    cursor: grab;
}
.title.input{
    background: var(--input-color);
    text-align: right;
}
.title.processor{
    background: var(--processor-color);
    text-align: center;
}
.title.output{
    background: var(--output-color);
    text-align: left;
}
.title.minimal{
    background: transparent;
    font-size: 20px;
    line-height: 50%;
    display: table;
    padding: 15px 0 15px 0;
    text-align: center;
    margin-top: auto;
    margin-bottom: auto;
}
.title.minimal[singlechar]{
    font-size: 32px;
}
.lock-icon{
    position: absolute;
    top: 0px;
    right: 0px;
    color: rgba(255,255,255,0.8);
    transform: scale(.7);
}
.lock-icon.input{
    left: 0;
}
`;
