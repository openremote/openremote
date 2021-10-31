import { css } from "lit";

export const PickerStyle = css`
input{
    border: 0;
}
textarea{
    min-width: 150px;
    min-height: 37px;
}
textarea, input[type=text], input[type=number], select{
    font-family: inherit;
    padding: 10px;
    border-radius: var(--roundness);    
    width: auto;
    border: none;
}
.attribute-label{
    padding: 5px;
    background: rgba(0,0,0,0.05);
    text-align: center;
    border-radius: var(--roundness);
    font-size: 110%;
    color: rgb(76, 76, 76);
    font-weight: 400;
}
`;
