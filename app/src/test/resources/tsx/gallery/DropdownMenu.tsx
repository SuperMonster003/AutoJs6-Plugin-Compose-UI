"ui";

let open = compose.state(false);

compose.mount(function () {
    return (
        <compose.Column padding={16}>
            <compose.DropdownMenu key='menu' open={open.value} onDismissRequest={function () { open.value = false; }} anchor={<compose.Button onClick={function () { open.value = true; }}>Open menu</compose.Button>}>
                <compose.DropdownMenuItem key='edit' text='Edit' onClick={function () { open.value = false; toast('Edit'); }} />
                <compose.DropdownMenuItem key='share' text='Share' onClick={function () { open.value = false; toast('Share'); }} />
            </compose.DropdownMenu>
        </compose.Column>
    );
});
