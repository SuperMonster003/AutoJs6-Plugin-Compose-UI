"ui";

let open = compose.state(false);

compose.mount(function () {
    return (
        <compose.Column padding={16}>
            <compose.Tooltip key='tip' tooltip='Long-press to show this tooltip' open={open.value} onOpenChange={function (value) { open.value = value; }}>
                <compose.Button onClick={function () { open.value = !open.value; }}>Hover me</compose.Button>
            </compose.Tooltip>
        </compose.Column>
    );
});
