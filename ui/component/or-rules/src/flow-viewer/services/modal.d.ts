import { PopupModal } from "../components/popup-modal";
import { TemplateResult } from "lit";
export declare class ModalService {
    element: PopupModal;
    confirmation(agreeCallback: () => void, header?: string, question?: string): void;
    notification(header: string, message: string, buttonText?: string): void;
    anything(header: string, content: TemplateResult): void;
}
