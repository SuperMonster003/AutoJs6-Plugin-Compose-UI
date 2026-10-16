"ui";

let layout = (
    <vertical>
        <text id='message' text='Hello' />
    </vertical>
);
ui.layout(layout);
ui.message.click(() => toast('hi'));
