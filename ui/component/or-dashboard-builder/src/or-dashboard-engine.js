import { GridStackEngine } from 'gridstack';
export class OrDashboardEngine extends GridStackEngine {
    // Cancelling move when it collides with the same widget.
    // Apparently during some rerenders, the widget moved downwards after colliding with itself.
    // Now cancelling the movement when the IDs are the same.
    moveNode(node, o) {
        if (o.skip && o.skip.id == node.id) {
            o.x = o.skip.x;
            o.y = o.skip.y;
            o.w = o.skip.w;
            o.h = o.skip.h;
        }
        return super.moveNode(node, o);
    }
}
//# sourceMappingURL=or-dashboard-engine.js.map