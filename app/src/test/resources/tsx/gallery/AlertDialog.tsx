"ui";

let open = compose.state(false);

compose.mount(function () {
    return (
        <compose.Column padding={16}>
            <compose.Button onClick={function () { open.value = true; }}>Show dialog</compose.Button>
            <compose.AlertDialog key='confirm' open={open.value} title='Delete file?' text='This cannot be undone.' confirm={<compose.TextButton onClick={function () { open.value = false; toast('Deleted'); }}>Delete</compose.TextButton>} dismiss={<compose.TextButton onClick={function () { open.value = false; }}>Cancel</compose.TextButton>} onDismissRequest={function () { open.value = false; }} />
        </compose.Column>
    );
});
