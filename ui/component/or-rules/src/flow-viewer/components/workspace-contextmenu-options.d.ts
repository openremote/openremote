import { EditorWorkspace } from "./editor-workspace";
import { ContextMenuButton, ContextMenuSeparator } from "../models/context-menu-button";
export declare const createContextMenuButtons: (workspace: EditorWorkspace, e: MouseEvent) => (ContextMenuButton | ContextMenuSeparator)[];
