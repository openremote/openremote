import { EventEmitter } from "events";
// Inspired by or-rules flow editor Input class
export class DashboardKeyEmitter extends EventEmitter {
    constructor() {
        super();
        /* ------------------------------------- */
        this.onkeydown = (e) => {
            if (e.key == 'Delete') {
                e.preventDefault();
                this.emit('delete', e);
            }
            else if (e.key == 'Escape') {
                e.preventDefault();
                this.emit('deselect', e);
            }
            else if (e.key == 's' && e.ctrlKey) {
                e.preventDefault();
                this.emit('save', e);
            }
        };
        window.addEventListener("keydown", this.onkeydown);
    }
}
//# sourceMappingURL=or-dashboard-keyhandler.js.map