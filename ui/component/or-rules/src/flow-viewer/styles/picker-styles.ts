import { css } from "lit";

export const PickerStyle = css`
input{
    border: 0;
    height: max-content;
}
textarea {
    min-width: 150px;
    min-height: 37px;
} 
input[type=number] {
    padding: 11px 10px;
}
textarea, input[type=text], select{
    font-family: inherit;
    padding: 10px;
    border-radius: var(--roundness);    
    width: fit-content;
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
