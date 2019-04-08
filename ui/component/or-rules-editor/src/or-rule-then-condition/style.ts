import {css} from 'lit-element';


export const style = css`
    :host {
        display: block;
    }
    
    .rule-container {
          display: flex;
          margin-bottom: 10px;
          padding: 5px 20px;
          
          border-radius: 5px;
          border: 1px solid var(--app-lightgrey-color, #F5F5F5);
    }
`;