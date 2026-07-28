# @openremote/or-scheduler \<or-scheduler\>
[![NPM Version][npm-image]][npm-url]

Web Component for displaying a scheduling dialog.

## Install
```bash
npm i @openremote/or-scheduler
yarn add @openremote/or-scheduler
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

The component renders a label describing the current schedule plus a dialog for editing it. Bind `schedule` to a
`CalendarEvent` and listen for changes; the component does not persist anything itself.

```html
<or-scheduler header="scheduleRuleActivity"
              .schedule="${this.ruleset.meta.validity}"
              @or-scheduler-changed="${(e) => this.validity = e.detail.value}"
              @or-scheduler-removed="${() => this.validity = undefined}">
</or-scheduler>
```

`header` and `defaultEventTypeLabel` are translation keys, the latter being the text shown while no schedule is set.
`open` controls the dialog, `removable` adds the action that clears the schedule, `isAllDay` starts the editor
without times, and `timezoneOffset` shifts the displayed times.

### Restricting the editor
`disabledFrequencies` removes frequencies such as `SECONDLY` from the picker, `disabledRRuleParts` removes individual
recurrence parts, and `disableNegativeByPartValues` suppresses the negative offsets, for example the last day of a
month.

`disabledByPartCombinations` maps each frequency to the parts that make no sense for it. Two presets are exported:
`RFC_STRICT_NOT_APPLICABLE` (the default) disables only what RFC 5545 forbids, while `INTUITIVE_NOT_APPLICABLE` also
hides combinations that are valid but confusing.

```html
<or-scheduler .disabledByPartCombinations="${INTUITIVE_NOT_APPLICABLE}"
              .disabledFrequencies="${["SECONDLY", "MINUTELY"]}"
              disableNegativeByPartValues>
</or-scheduler>
```

### Events
* `or-scheduler-changed` (`OrSchedulerChangedEvent`) - The schedule changed; detail contains the new `value`
* `or-scheduler-removed` (`OrSchedulerRemovedEvent`) - The schedule was cleared

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge. In addition,
Internet Explorer 11 is also supported.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-scheduler.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-scheduler
