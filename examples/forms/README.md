# Live Form Events Example

The example app exposes a runnable form event page at:

```text
http://localhost:8080/forms
```

It demonstrates:

- `onInput(Consumer<String>)` for text input values.
- `onInput(Consumer<String>)` for textarea values.
- `onChange(Consumer<String>)` for select values.
- checkbox values, including the empty value sent when unchecked.
- radio group values, ignoring unchecked radio events.
- `select multiple` values sent in DOM order separated by newlines.
- `onSubmit(Runnable)` for server-side form submit handling without a browser page reload.
- live re-rendering after each event.

Build the example app from the repository root:

```bash
./mvnw -pl examples -am package
```

Then run `app.Main` from your IDE or preferred Java launcher.

Open the page, type in the name and notes fields, change the profile, select multiple interests, toggle the checkbox, switch the plan, and submit the form. The state panel updates from server-side signals after each live event.

The live browser bridge sends opaque event ids and a string `value` field to `/_ujfe/event`. Application code never receives DOM event ids directly and does not need client-side JavaScript for ordinary form handling.
