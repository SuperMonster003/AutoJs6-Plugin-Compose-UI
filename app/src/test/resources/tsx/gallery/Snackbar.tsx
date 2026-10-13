"ui";

let session = compose.mount(function () {
    return (
        <compose.Scaffold>
            <compose.Column padding={16}>
                <compose.Button key='show' onClick={function () { session.showSnackbar('Saved', {actionLabel: 'Undo', onAction: function () { toast('Undone'); }}); }}>Show snackbar</compose.Button>
            </compose.Column>
        </compose.Scaffold>
    );
});
