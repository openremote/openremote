import type { Meta, StoryObj } from '@storybook/web-components';
import { getWcStorybookHelpers } from "wc-storybook-helpers";
import {OrMwcTable} from "@openremote/or-mwc-components/or-mwc-table";
import "@openremote/or-mwc-components/or-mwc-table";
import {getMarkdownString, splitLines} from "./util/util";
import ReadMe from "./temp/or-mwc-components-README.md";

const { events, args, argTypes, template } = getWcStorybookHelpers("or-mwc-table");

console.log(argTypes);

const meta: Meta<OrMwcTable> = {
    title: "Components/or-mwc-table",
    component: "or-mwc-table",
    args: args,
    argTypes: argTypes,
    parameters: {
        actions: {
            handles: events
        }
    }
};

type Story = StoryObj<OrMwcTable & typeof args>;

export const Primary: Story = {
    args: {
        columns: [{title: "Column 1"}, {title: "Column 2"}, {title: "Column 3"}],
        rows: [
            {content: ["Row 1, Column 1", "Row 1, Column 2", "Row 1, Column 3"]},
            {content: ["Row 2, Column 1", "Row 2, Column 2", "Row 2, Column 3"]}
        ]
    },
    parameters: {
        docs: {
            readmeStr: splitLines(getMarkdownString(ReadMe), 1),
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
