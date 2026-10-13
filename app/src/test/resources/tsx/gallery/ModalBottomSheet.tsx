"ui";

let open = compose.state(false);

compose.mount(function () {
    return (
        <compose.Column padding={16}>
            <compose.Button onClick={function () { open.value = true; }}>Show bottom sheet</compose.Button>
            <compose.ModalBottomSheet key='sheet' open={open.value} onDismissRequest={function () { open.value = false; }}>
                <compose.Column padding={24} spacing={12}>
                    <compose.Text key='title' text='Sheet title' style='titleMedium' />
                    <compose.Button onClick={function () { open.value = false; }}>Close</compose.Button>
                </compose.Column>
            </compose.ModalBottomSheet>
        </compose.Column>
    );
});
