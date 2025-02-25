import { getWcStorybookHelpers } from "wc-storybook-helpers";
import "@openremote/or-mwc-components/or-mwc-table";

const { events, args, argTypes, template } = getWcStorybookHelpers("or-mwc-table");

/** @type { import('@storybook/web-components').Meta } */
const meta = {
    title: "Playground/or-mwc-table",
    component: "or-mwc-table",
    args: args,
    argTypes: argTypes,
    parameters: {
        actions: {
            handles: events
        },
        docs: {
          subtitle: "<or-mwc-table>"
        }
    }
};

/** @type { import('@storybook/web-components').StoryObj } */
export const Primary = {
    args: {
        columns: JSON.stringify([{title: "Column 1"}, {title: "Column 2"}, {title: "Column 3"}]),
        rows: JSON.stringify([
            {content: ["Row 1, Column 1", "Row 1, Column 2", "Row 1, Column 3"]},
            {content: ["Row 2, Column 1", "Row 2, Column 2", "Row 2, Column 3"]}
        ])
    },
    parameters: {
        docs: {
            source: {
                code: getExampleCode()
            },
            story: {
                height: '240px'
            }
        }
    },
    render: (args) => template(args)
};

/* ------------------------------------------------------- */
/*                   UTILITY FUNCTIONS                     */
/* ------------------------------------------------------- */

function getExampleCode() {

    //language=javascript
    return `
import {TableColumn, TableRow} from "@openremote/or-mwc-components/or-mwc-table";
import "@openremote/or-mwc-components/or-mwc-table"; // this is necessary

// Set up table columns
const columns: TableColumn[] = [{title: "Column 1"}, {title: "Column 2"}, {title: "Column 3"}];

// Set up table rows
const rows: TableRow[] = [
    {content: ["Row 1, Column 1", "Row 1, Column 2", "Row 1, Column 3"]},
    {content: ["Row 2, Column 1", "Row 2, Column 2", "Row 2, Column 3"]}
];

// (IF NOT USING LIT; you should parse the objects to JSON strings)
// const columnsStr = JSON.stringify(columns);
// const rowsStr = JSON.stringify(rows);

// in your HTML code use this, and inject them;
<or-mwc-table columns="columns" rows="rows"></or-mwc-table>
`
}

export default meta;
