"ui";

let showDone = compose.state(true);
let showOpen = compose.state(false);

compose.mount(function () {
    return (
        <compose.Row padding={16} spacing={8}>
            <compose.FilterChip key='done' label='Done' selected={showDone.value} onSelectedChange={function (value) { showDone.value = value; }} />
            <compose.FilterChip key='open' label='Open' selected={showOpen.value} onSelectedChange={function (value) { showOpen.value = value; }} />
        </compose.Row>
    );
});
