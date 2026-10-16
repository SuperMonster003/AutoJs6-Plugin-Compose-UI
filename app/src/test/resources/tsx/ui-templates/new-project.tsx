"ui";

ui.layout(
    <vertical padding='16'>
        <text id='title' text='Hello' textSize='24sp' />
        <button id='ok' text='OK' onClick={() => {
            toast('OK');
        }} />
    </vertical>
);
