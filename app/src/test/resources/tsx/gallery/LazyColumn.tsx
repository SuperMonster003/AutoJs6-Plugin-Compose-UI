"ui";

let rows = [];
for (let i = 0; i < 100; i++) rows.push(compose.Text({key: 'row-' + i, text: 'Row ' + i}));

compose.mount(function () {
    return (
        <compose.LazyColumn contentPadding={16} spacing={8}>
            {rows}
        </compose.LazyColumn>
    );
});
