declare module "@polymer/app-localize-behavior/app-localize-behavior.js" {
    class AppLocalizeBehavior {

        /**
         * The language used for translation.
         */
        language: string | null | undefined;

        /**
         * The dictionary of localized messages, for each of the languages that
         * are going to be used. See http://formatjs.io/guides/message-syntax/ for
         * more information on the message syntax.
         *
         * For example, a valid dictionary would be:
         * this.resources = {
         *  'en': { 'greeting': 'Hello!' }, 'fr' : { 'greeting': 'Bonjour!' }
         * }
         */
        resources: object | null | undefined;

        /**
         * Optional dictionary of user defined formats, as explained here:
         * http://formatjs.io/guides/message-syntax/#custom-formats
         *
         * For example, a valid dictionary of formats would be:
         * this.formats = {
         *    number: { USD: { style: 'currency', currency: 'USD' } }
         * }
         */
        formats: object | null | undefined;

        /**
         * If true, will use the provided key when
         * the translation does not exist for that key.
         */
        useKeyIfMissing: boolean | null | undefined;

        /**
         * Translates a string to the current `language`. Any parameters to the
         * string should be passed in order, as follows:
         * `localize(stringKey, param1Name, param1Value, param2Name, param2Value)`
         */
        readonly localize: Function | null | undefined;

        /**
         * If true, will bubble up the event to the parents
         */
        bubbleEvent: boolean | null | undefined;

        loadResources(path: any, language: any, merge: any): any;
    }
}
