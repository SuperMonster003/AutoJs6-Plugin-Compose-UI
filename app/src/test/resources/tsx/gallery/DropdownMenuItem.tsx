"ui";

let open = compose.state(false);

compose.mount(function () {
    return (
        <compose.Column padding={16}>
            <compose.DropdownMenu key='menu' open={open.value} onDismissRequest={function () { open.value = false; }} anchor={<compose.IconButton onClick={function () { open.value = true; }}>
                <compose.Icon name='Filled.MoreVert' />
            </compose.IconButton>}>
                <compose.DropdownMenuItem key='copy' text='Copy' leadingIcon={<compose.Icon name='Filled.Share' />} onClick={function () { open.value = false; }} />
                <compose.DropdownMenuItem key='delete' text='Delete' leadingIcon={<compose.Icon name='Filled.Delete' />} enabled={false} />
            </compose.DropdownMenu>
        </compose.Column>
    );
});
