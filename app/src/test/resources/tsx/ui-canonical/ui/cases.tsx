"ui";

// Hand-written TSX ui cases for the designer (roadmap P11.7).
const title: string = 'Cases';
ui.useAndroidLayout(false);
ui.registerWidget('my-widget', {});

ui.layout(
    <frame>
        {/* The root holds a vertical list. */}
        <vertical id='list' padding={8} margin={-4} alpha={0.5} visibility='visible' clickable enabled={true}>
            <text id='title' text={title} textColor='#FF0000' />
            <text id='entities'>
                {'Tom & Jerry <3'}
            </text>
            <text id='bound' text='{{count}} items' />
            <input id='name' hint={'Name: \'first\''} lines={2} />
            <button id='ok' text='OK' onClick={() => {
                toast('OK');
            }} />
            <button id='cancel' w='*' android:id='@+id/cancel'>Cancel</button>
        </vertical>
    </frame>
);

(ui as any).list.on('long_click', (_event: any, _view: any) => {
    toast('long');
});
ui.findView('cancel')?.click(() => {
    toast('cancel');
});

const row = (
    <horizontal>
        <text text='row' />
    </horizontal>
);
ui.inflate(row, (ui as any).list, true);
