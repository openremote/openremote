import { GridStackEngine, GridStackNode, GridStackMoveOpts } from 'gridstack';

export class OrDashboardEngine extends GridStackEngine {

    /** refined this to move the node to the given new location */
    public moveNode(node: GridStackNode, o: GridStackMoveOpts): boolean {
        if(o.skip && o.skip.id == node.id) {
            o.x = o.skip.x;
            o.y = o.skip.y;
            o.w = o.skip.w;
            o.h = o.skip.h;
        }
        return super.moveNode(node, o);
    }
}
