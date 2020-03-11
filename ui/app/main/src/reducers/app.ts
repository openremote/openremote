import { Reducer } from "redux";
import {
    UPDATE_PAGE,
    SET_SCROLL_TOP,
    UPDATE_DRAWER,
    RESOLVE_APP,
    AppAction, SET_ACTIVE_ASSET
} from "../actions/app";

export interface AppState {
  page: string;
  activeAsset: string;
  offline: boolean;
  resolved: boolean,
  drawerOpened: boolean;
  scrollTop: number;
}

const INITIAL_STATE: AppState = {
  page: "",
  activeAsset: undefined,
  offline: false,
  resolved: false,
  drawerOpened: false,
  scrollTop: 0,
};

const app: Reducer<AppState, AppAction> = (state = INITIAL_STATE, action) => {
  switch (action.type) {
    case UPDATE_PAGE:
      return {
        ...state,
        page: action.page
      };
    case RESOLVE_APP:
        return {
            ...state,
            resolved: action.resolved
        };
    case UPDATE_DRAWER:
      return {
        ...state,
        drawerOpened: action.opened
      };
    case SET_SCROLL_TOP:
        return {
            ...state,
            scrollTop: action.top
        };
      case SET_ACTIVE_ASSET:
          return {
              ...state,
              activeAsset: action.assetId
          };
    default:
      return state;
  }
};

export default app;
