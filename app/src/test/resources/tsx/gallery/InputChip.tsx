"ui";

let selected = compose.state(false);

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={12}>
            <compose.InputChip key='tag' label='Android' selected={selected.value} leadingIcon={<compose.Icon name='Filled.Check' />} onSelectedChange={function (value) { selected.value = value; }} />
        </compose.Column>
    );
});
